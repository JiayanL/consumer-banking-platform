import express from 'express';
import request from 'supertest';
import { rateLimit } from './rateLimit';

function buildApp(max: number) {
  const app = express();
  app.use(rateLimit({ windowMs: 60_000, max }));
  app.get('/test', (_req, res) => res.json({ ok: true }));
  return app;
}

describe('rateLimit', () => {
  test('requests within limit pass through', async () => {
    const app = buildApp(5);
    const res = await request(app).get('/test');
    expect(res.status).toBe(200);
  });

  test('requests exceeding limit get 429', async () => {
    const app = buildApp(2);
    await request(app).get('/test');
    await request(app).get('/test');
    const res = await request(app).get('/test');
    expect(res.status).toBe(429);
    expect(res.body.error).toMatch(/rate limit/i);
    expect(res.headers['retry-after']).toBeDefined();
  });
});
