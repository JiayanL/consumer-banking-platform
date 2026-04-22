import { Request, Response, NextFunction } from 'express';
import { randomUUID } from 'node:crypto';
import { logger } from '../logger';
import type { Logger } from '@cbp/logging-sdk';

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      requestId?: string;
      logger?: Logger;
    }
  }
}

export function requestIdMiddleware(req: Request, res: Response, next: NextFunction): void {
  const existing = req.header('x-request-id');
  const id = typeof existing === 'string' && existing.length > 0 ? existing : randomUUID();
  req.requestId = id;
  req.logger = logger.child({ requestId: id });
  res.setHeader('x-request-id', id);
  next();
}
