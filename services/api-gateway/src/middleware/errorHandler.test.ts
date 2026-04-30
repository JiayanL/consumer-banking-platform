import express, { Request, Response, NextFunction } from 'express';
import request from 'supertest';
import { HttpError, errorHandler, notFoundHandler } from './errorHandler';

function buildApp(throwFn: (req: Request, res: Response, next: NextFunction) => void) {
  const app = express();
  app.get('/test', throwFn);
  app.use(notFoundHandler);
  app.use(errorHandler);
  return app;
}

describe('errorHandler', () => {
  test('HttpError returns correct status and message', async () => {
    const app = buildApp((_req, _res, next) => {
      next(new HttpError(400, 'bad request', { field: 'email' }));
    });
    const res = await request(app).get('/test');
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('bad request');
    expect(res.body.details).toEqual({ field: 'email' });
  });

  test('generic Error returns 500', async () => {
    const app = buildApp((_req, _res, next) => {
      next(new Error('something broke'));
    });
    const res = await request(app).get('/test');
    expect(res.status).toBe(500);
    expect(res.body.error).toBe('internal error');
  });

  test('non-Error value returns 500', async () => {
    const app = buildApp((_req, _res, next) => {
      next('string-error');
    });
    const res = await request(app).get('/test');
    expect(res.status).toBe(500);
  });
});

describe('notFoundHandler', () => {
  test('returns 404 with path', async () => {
    const app = express();
    app.use(notFoundHandler);
    const res = await request(app).get('/missing');
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('not found');
    expect(res.body.path).toBe('/missing');
  });
});
