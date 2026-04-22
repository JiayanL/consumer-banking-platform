import express, { Request, Response, NextFunction } from 'express';
import { logger } from './logger';
import { CustomerStore } from './store';
import { isEmail, isPhone, requireString, requireBoolean, ValidationError } from './validators';
import { CustomerPatch } from './types';

export interface AppDeps {
  customers: CustomerStore;
}

export function createApp(deps: AppDeps = { customers: new CustomerStore() }) {
  const app = express();
  app.use(express.json());

  app.post('/customers', (req: Request, res: Response, next: NextFunction) => {
    try {
      const b = req.body ?? {};
      const firstName = requireString(b.firstName, 'firstName');
      const lastName = requireString(b.lastName, 'lastName');
      const email = requireString(b.email, 'email');
      if (!isEmail(email)) {
        return res.status(400).json({ error: 'invalid email' });
      }
      let phone: string | undefined;
      if (b.phone !== undefined) {
        if (!isPhone(b.phone)) return res.status(400).json({ error: 'invalid phone' });
        phone = b.phone;
      }
      const c = deps.customers.create({ firstName, lastName, email, phone });
      logger.info('customer.created', { id: c.id });
      return res.status(201).json(c);
    } catch (err) {
      return next(err);
    }
  });

  app.get('/customers/:id', (req: Request, res: Response) => {
    const c = deps.customers.get(req.params.id);
    if (!c) return res.status(404).json({ error: 'not found' });
    return res.json(c);
  });

  /** @compliance-critical PII_HANDLING */
  app.patch('/customers/:id', (req: Request, res: Response, next: NextFunction) => {
    try {
      const b = req.body ?? {};
      const patch: CustomerPatch = {};
      if (b.firstName !== undefined) patch.firstName = requireString(b.firstName, 'firstName');
      if (b.lastName !== undefined) patch.lastName = requireString(b.lastName, 'lastName');
      if (b.email !== undefined) {
        if (!isEmail(b.email)) return res.status(400).json({ error: 'invalid email' });
        patch.email = b.email;
      }
      if (b.phone !== undefined) {
        if (b.phone === null) {
          patch.phone = undefined;
        } else {
          if (!isPhone(b.phone)) return res.status(400).json({ error: 'invalid phone' });
          patch.phone = b.phone;
        }
      }
      const updated = deps.customers.patch(req.params.id, patch);
      if (!updated) return res.status(404).json({ error: 'not found' });
      logger.info('customer.updated', { id: updated.id, fields: Object.keys(patch) });
      return res.json(updated);
    } catch (err) {
      return next(err);
    }
  });

  app.get('/customers/:id/preferences', (req: Request, res: Response) => {
    const c = deps.customers.get(req.params.id);
    if (!c) return res.status(404).json({ error: 'not found' });
    const prefs = deps.customers.getPreferences(req.params.id);
    if (!prefs) {
      return res.json({
        customerId: req.params.id,
        emailOptIn: true,
        smsOptIn: false,
        marketingOptIn: false,
        locale: 'en-US',
        updatedAt: c.updatedAt,
      });
    }
    return res.json(prefs);
  });

  app.put('/customers/:id/preferences', (req: Request, res: Response, next: NextFunction) => {
    try {
      const c = deps.customers.get(req.params.id);
      if (!c) return res.status(404).json({ error: 'not found' });
      const b = req.body ?? {};
      const prefs = deps.customers.putPreferences(req.params.id, {
        emailOptIn: requireBoolean(b.emailOptIn, 'emailOptIn'),
        smsOptIn: requireBoolean(b.smsOptIn, 'smsOptIn'),
        marketingOptIn: requireBoolean(b.marketingOptIn, 'marketingOptIn'),
        locale: requireString(b.locale, 'locale'),
      });
      logger.info('customer.preferences.updated', { id: req.params.id });
      return res.json(prefs);
    } catch (err) {
      return next(err);
    }
  });

  app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
    if (err instanceof ValidationError) {
      return res.status(400).json({ error: err.message });
    }
    logger.error('unhandled', { err: err.message });
    return res.status(500).json({ error: 'internal error' });
  });

  return app;
}
