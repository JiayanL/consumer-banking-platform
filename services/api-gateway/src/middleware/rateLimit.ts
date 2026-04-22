import { Request, Response, NextFunction } from 'express';

export interface RateLimitOptions {
  windowMs: number;
  max: number;
  keyFn?: (req: Request) => string;
}

interface Bucket {
  count: number;
  resetAt: number;
}

/**
 * In-memory token-bucket rate limiter. Real deployments use Redis;
 * this is here to keep the middleware chain representative for tests
 * and local dev. Not wired into the router yet — see TODO in auth.ts.
 */
export function rateLimit(opts: RateLimitOptions) {
  const buckets = new Map<string, Bucket>();
  const keyFn = opts.keyFn ?? ((req: Request) => req.ip ?? 'unknown');

  return function rateLimitMiddleware(req: Request, res: Response, next: NextFunction) {
    const key = keyFn(req);
    const now = Date.now();
    const existing = buckets.get(key);
    if (!existing || existing.resetAt <= now) {
      buckets.set(key, { count: 1, resetAt: now + opts.windowMs });
      return next();
    }
    if (existing.count >= opts.max) {
      const retryAfterSec = Math.ceil((existing.resetAt - now) / 1000);
      res.setHeader('Retry-After', String(retryAfterSec));
      return res.status(429).json({ error: 'rate limit exceeded' });
    }
    existing.count += 1;
    return next();
  };
}

export function clearAllBuckets(limiter: ReturnType<typeof rateLimit>): void {
  // Reserved hook for tests. The limiter closes over its own map, so
  // tests can create a fresh instance per run instead of poking at it.
  void limiter;
}
