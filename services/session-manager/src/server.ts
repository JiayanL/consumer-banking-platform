import express, { Request, Response } from 'express';
import { createLogger } from '@cbp/logging-sdk';

import { SessionStore } from './sessionStore';
import { issueToken, verifyToken } from './tokens';

const log = createLogger({ name: 'session-manager' });
const store = new SessionStore();
const SECRET = process.env.CBP_AUTH_SECRET ?? 'dev-stub-secret-do-not-use-in-prod';

export function buildApp(): express.Express {
  const app = express();
  app.use(express.json());

  app.get('/healthz', (_req: Request, res: Response) => {
    res.json({ status: 'ok', sessions: store.size() });
  });

  app.post('/sessions', (req: Request, res: Response) => {
    const { userId, roles } = req.body ?? {};
    if (!userId) return res.status(400).json({ error: 'userId required' });
    const now = Math.floor(Date.now() / 1000);
    const session = store.create({
      userId,
      roles: roles ?? [],
      expiresAt: (now + 15 * 60) * 1000,
      idleTimeoutMs: 30 * 60 * 1000,
    });
    const token = issueToken(SECRET, { sub: userId, roles: session.roles, sid: session.id });
    log.info('session.created', { userId, sid: session.id });
    return res.status(201).json({ session, token });
  });

  app.get('/sessions/current', (req: Request, res: Response) => {
    const authz = req.headers.authorization;
    const result = verifyToken(SECRET, authz);
    if (!result.valid) return res.status(401).json({ error: result.reason });
    const session = store.touch(result.claims.sid);
    if (!session) return res.status(404).json({ error: 'session not found' });
    return res.json(session);
  });

  app.delete('/sessions/current', (req: Request, res: Response) => {
    const result = verifyToken(SECRET, req.headers.authorization);
    if (!result.valid) return res.status(401).json({ error: result.reason });
    store.revoke(result.claims.sid);
    return res.status(204).send();
  });

  app.post('/sessions/refresh', (req: Request, res: Response) => {
    const result = verifyToken(SECRET, req.headers.authorization);
    if (!result.valid) return res.status(401).json({ error: result.reason });
    const session = store.get(result.claims.sid);
    if (!session) return res.status(404).json({ error: 'session not found' });
    const token = issueToken(SECRET, {
      sub: session.userId,
      roles: session.roles,
      sid: session.id,
    });
    return res.json({ token });
  });

  return app;
}

if (require.main === module) {
  const port = Number(process.env.PORT ?? 8090);
  buildApp().listen(port, () => log.info('session-manager.listening', { port }));
}
