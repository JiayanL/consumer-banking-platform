import express from 'express';
import request from 'supertest';
import { authMiddleware } from './auth';

function buildApp() {
  const app = express();
  app.use('/api', authMiddleware);
  app.get('/api/test', (_req, res) => res.json({ ok: true }));
  return app;
}

describe('authMiddleware', () => {
  test('rejects requests without Authorization header', async () => {
    const res = await request(buildApp()).get('/api/test');
    expect(res.status).toBe(401);
    expect(res.body.error).toMatch(/authorization/i);
  });

  test('rejects requests with empty Authorization header', async () => {
    const res = await request(buildApp())
      .get('/api/test')
      .set('Authorization', '   ');
    expect(res.status).toBe(401);
  });

  test('allows requests with valid Authorization header', async () => {
    const res = await request(buildApp())
      .get('/api/test')
      .set('Authorization', 'Bearer some-token');
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
  });
});
