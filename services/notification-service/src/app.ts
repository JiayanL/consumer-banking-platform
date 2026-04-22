import express, { Request, Response, NextFunction } from 'express';
import { logger } from './logger';
import { NotificationStore, TemplateStore, Channel } from './store';
import { renderTemplate, interpolate, Template } from './templates';
import { isEmail, isPhone, ValidationError } from './validators';

export interface AppDeps {
  notifications: NotificationStore;
  templates: TemplateStore;
}

export function createApp(deps: AppDeps = { notifications: new NotificationStore(), templates: new TemplateStore() }) {
  const app = express();
  app.use(express.json());

  app.post('/notify/email', (req: Request, res: Response, next: NextFunction) => {
    try {
      const { to, subject, body, templateId, vars } = req.body ?? {};
      if (!isEmail(to)) {
        return res.status(400).json({ error: 'invalid `to` address' });
      }
      const rendered = resolveContent({ subject, body, templateId, vars }, deps.templates);
      const record = deps.notifications.create({
        channel: 'email',
        to,
        subject: rendered.subject,
        body: rendered.body,
        templateId,
      });
      logger.info('email.queued', { id: record.id, to });
      return res.status(202).json(record);
    } catch (err) {
      return next(err);
    }
  });

  app.post('/notify/sms', (req: Request, res: Response, next: NextFunction) => {
    try {
      const { to, body, templateId, vars } = req.body ?? {};
      if (!isPhone(to)) {
        return res.status(400).json({ error: 'invalid `to` number' });
      }
      const rendered = resolveContent({ body, templateId, vars }, deps.templates);
      const record = deps.notifications.create({
        channel: 'sms' as Channel,
        to,
        body: rendered.body,
        templateId,
      });
      logger.info('sms.queued', { id: record.id, to });
      return res.status(202).json(record);
    } catch (err) {
      return next(err);
    }
  });

  app.get('/notifications/:id', (req: Request, res: Response) => {
    const rec = deps.notifications.get(req.params.id);
    if (!rec) return res.status(404).json({ error: 'not found' });
    return res.json(rec);
  });

  app.get('/templates', (_req: Request, res: Response) => {
    return res.json(deps.templates.list());
  });

  app.post('/templates', (req: Request, res: Response, next: NextFunction) => {
    try {
      const { id, subject, body } = req.body ?? {};
      if (typeof id !== 'string' || id.length === 0) {
        return res.status(400).json({ error: '`id` is required' });
      }
      if (typeof body !== 'string') {
        return res.status(400).json({ error: '`body` is required' });
      }
      const tmpl: Template = { id, subject, body };
      deps.templates.upsert(tmpl);
      logger.info('template.upserted', { id });
      return res.status(201).json(tmpl);
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

function resolveContent(
  input: { subject?: unknown; body?: unknown; templateId?: unknown; vars?: unknown },
  templates: TemplateStore,
): { subject: string; body: string } {
  const vars = (input.vars && typeof input.vars === 'object' ? input.vars : {}) as Record<string, unknown>;
  if (typeof input.templateId === 'string' && input.templateId.length > 0) {
    const tmpl = templates.get(input.templateId);
    if (!tmpl) throw new ValidationError(`unknown templateId: ${input.templateId}`);
    return renderTemplate(tmpl, vars);
  }
  if (typeof input.body !== 'string') {
    throw new ValidationError('`body` or `templateId` is required');
  }
  const subject = typeof input.subject === 'string' ? interpolate(input.subject, vars) : '';
  const body = interpolate(input.body, vars);
  return { subject, body };
}
