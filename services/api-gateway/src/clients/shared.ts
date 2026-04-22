export interface ClientOptions {
  baseUrl: string;
  timeoutMs?: number;
  retries?: number;
}

export interface RequestContext {
  requestId: string;
  authorization?: string;
  correlationId?: string;
}

/**
 * Shared helpers used by all stub clients. In the real implementation
 * this would wrap a fetch/undici call with structured retries — here
 * it's just a place to keep the shape consistent across clients.
 */
export function buildHeaders(ctx: RequestContext): Record<string, string> {
  const headers: Record<string, string> = {
    'x-request-id': ctx.requestId,
    accept: 'application/json',
    'content-type': 'application/json',
  };
  if (ctx.authorization) headers.authorization = ctx.authorization;
  if (ctx.correlationId) headers['x-correlation-id'] = ctx.correlationId;
  return headers;
}

export function applyTimeout<T>(p: Promise<T>, timeoutMs: number): Promise<T> {
  let timer: NodeJS.Timeout | undefined;
  const timeout = new Promise<T>((_, reject) => {
    timer = setTimeout(() => reject(new Error(`upstream timeout after ${timeoutMs}ms`)), timeoutMs);
  });
  return Promise.race([p, timeout]).finally(() => {
    if (timer) clearTimeout(timer);
  });
}

export async function withRetry<T>(
  fn: () => Promise<T>,
  opts: { retries: number; backoffMs: number } = { retries: 2, backoffMs: 50 },
): Promise<T> {
  let lastErr: unknown;
  for (let attempt = 0; attempt <= opts.retries; attempt++) {
    try {
      return await fn();
    } catch (err) {
      lastErr = err;
      if (attempt < opts.retries) {
        await new Promise((r) => setTimeout(r, opts.backoffMs * Math.pow(2, attempt)));
      }
    }
  }
  throw lastErr ?? new Error('request failed');
}

export function normaliseBaseUrl(url: string): string {
  return url.endsWith('/') ? url.slice(0, -1) : url;
}
