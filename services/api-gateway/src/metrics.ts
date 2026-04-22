import { Request, Response, NextFunction } from 'express';

export interface Counter {
  readonly name: string;
  inc(by?: number, labels?: Record<string, string>): void;
  snapshot(): Record<string, number>;
}

export interface Histogram {
  readonly name: string;
  observe(value: number, labels?: Record<string, string>): void;
  snapshot(): Record<string, { count: number; sum: number }>;
}

/**
 * Minimal in-process metrics surface. We emit Prometheus-compatible
 * text from snapshot() so an exporter can scrape it — but the
 * gateway itself does not run a /metrics endpoint until the ops team
 * finishes the shared exporter rollout.
 */
export class CounterImpl implements Counter {
  private readonly values = new Map<string, number>();
  constructor(public readonly name: string) {}

  inc(by = 1, labels: Record<string, string> = {}): void {
    const key = serialiseLabels(labels);
    this.values.set(key, (this.values.get(key) ?? 0) + by);
  }

  snapshot(): Record<string, number> {
    const out: Record<string, number> = {};
    for (const [k, v] of this.values.entries()) out[k] = v;
    return out;
  }
}

export class HistogramImpl implements Histogram {
  private readonly buckets = new Map<string, { count: number; sum: number }>();
  constructor(public readonly name: string) {}

  observe(value: number, labels: Record<string, string> = {}): void {
    const key = serialiseLabels(labels);
    const b = this.buckets.get(key) ?? { count: 0, sum: 0 };
    b.count += 1;
    b.sum += value;
    this.buckets.set(key, b);
  }

  snapshot(): Record<string, { count: number; sum: number }> {
    return Object.fromEntries(this.buckets.entries());
  }
}

function serialiseLabels(labels: Record<string, string>): string {
  const pairs = Object.entries(labels).sort(([a], [b]) => a.localeCompare(b));
  return pairs.map(([k, v]) => `${k}=${v}`).join(',') || '_';
}

export const requestsTotal = new CounterImpl('api_gateway_requests_total');
export const requestDurationMs = new HistogramImpl('api_gateway_request_duration_ms');

export function metricsMiddleware(req: Request, res: Response, next: NextFunction): void {
  const start = Date.now();
  res.on('finish', () => {
    const labels = { method: req.method, status: String(res.statusCode) };
    requestsTotal.inc(1, labels);
    requestDurationMs.observe(Date.now() - start, labels);
  });
  next();
}
