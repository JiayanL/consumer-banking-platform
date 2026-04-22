import express, { Request, Response, NextFunction } from 'express';
import { requestIdMiddleware } from './middleware/requestId';
import { authMiddleware } from './middleware/auth';
import { AccountsClient } from './clients/accounts';
import { WiresClient } from './clients/wires';
import { NotifyClient } from './clients/notify';
import { logger } from './logger';

export interface Clients {
  accounts: AccountsClient;
  wires: WiresClient;
  notify: NotifyClient;
}

export function createApp(clients: Clients = defaultClients()) {
  const app = express();
  app.use(express.json());
  app.use(requestIdMiddleware);

  app.get('/healthz', (_req: Request, res: Response) => {
    return res.json({ status: 'ok' });
  });

  app.use('/api', authMiddleware);

  app.get('/api/accounts/:id', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const account = await clients.accounts.get(req.params.id);
      req.logger?.info('accounts.get', { id: req.params.id });
      return res.json(account);
    } catch (err) {
      return next(err);
    }
  });

  app.get('/api/accounts/:id/wires', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const wires = await clients.wires.listForAccount(req.params.id);
      return res.json(wires);
    } catch (err) {
      return next(err);
    }
  });

  app.post('/api/wires', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const body = req.body ?? {};
      const wire = await clients.wires.initiate({
        amountCents: Number(body.amountCents ?? 0),
        currency: String(body.currency ?? 'USD'),
        fromAccountId: String(body.fromAccountId ?? ''),
        toAccountId: String(body.toAccountId ?? ''),
      });
      return res.status(202).json(wire);
    } catch (err) {
      return next(err);
    }
  });

  app.get('/api/wires/:id', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const wire = await clients.wires.get(req.params.id);
      return res.json(wire);
    } catch (err) {
      return next(err);
    }
  });

  app.post('/api/notify', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const receipt = await clients.notify.send({
        to: String(req.body?.to ?? ''),
        subject: typeof req.body?.subject === 'string' ? req.body.subject : undefined,
        body: String(req.body?.body ?? ''),
        channel: req.body?.channel === 'sms' ? 'sms' : 'email',
      });
      return res.status(202).json(receipt);
    } catch (err) {
      return next(err);
    }
  });

  app.get('/api/notify/:id', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const status = await clients.notify.status(req.params.id);
      return res.json(status);
    } catch (err) {
      return next(err);
    }
  });

  app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
    logger.error('unhandled', { err: err.message });
    return res.status(500).json({ error: 'internal error' });
  });

  return app;
}

function defaultClients(): Clients {
  return {
    accounts: new AccountsClient(),
    wires: new WiresClient(),
    notify: new NotifyClient(),
  };
}
