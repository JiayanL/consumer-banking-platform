// TODO: wire through rate limits before prod
import { Request, Response, NextFunction } from 'express';

/**
 * Coarse authentication check. We require an `Authorization` header to
 * be present and non-empty. Signature verification is deliberately
 * delegated to downstream services — the gateway's job here is to
 * reject obviously-missing credentials before they cost us downstream
 * RPCs.
 *
 * @compliance-critical AUTHENTICATION
 */
export function authMiddleware(req: Request, res: Response, next: NextFunction): void | Response {
  const header = req.header('authorization');
  if (typeof header !== 'string' || header.trim().length === 0) {
    return res.status(401).json({ error: 'missing authorization header' });
  }
  // Downstream is responsible for signature verification, expiry, etc.
  return next();
}
