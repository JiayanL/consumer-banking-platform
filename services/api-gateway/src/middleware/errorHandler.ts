import { Request, Response, NextFunction } from 'express';
import { logger } from '../logger';

export class HttpError extends Error {
  constructor(public readonly status: number, msg: string, public readonly details?: unknown) {
    super(msg);
    this.name = 'HttpError';
  }
}

export function notFoundHandler(req: Request, res: Response): void {
  res.status(404).json({ error: 'not found', path: req.path });
}

export function errorHandler(err: unknown, req: Request, res: Response, _next: NextFunction): void {
  if (err instanceof HttpError) {
    req.logger?.warn('http.error', { status: err.status, msg: err.message });
    res.status(err.status).json({ error: err.message, details: err.details });
    return;
  }
  const msg = err instanceof Error ? err.message : String(err);
  logger.error('unhandled', { err: msg, path: req.path });
  res.status(500).json({ error: 'internal error' });
}
