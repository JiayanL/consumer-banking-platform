import express from 'express';
import request from 'supertest';
import { requestIdMiddleware } from './requestId';

function buildApp() {
  const app = express();
  app.use(requestIdMiddleware);
  app.get('/test', (req, res) => res.json({ requestId: req.requestId }));
  return app;
}

describe('requestIdMiddleware', () => {
  test('adds x-request-id header to response', async () => {
    const res = await request(buildApp()).get('/test');
    expect(res.headers['x-request-id']).toBeDefined();
    expect(typeof res.headers['x-request-id']).toBe('string');
  });

  test('preserves existing x-request-id if present', async () => {
    const res = await request(buildApp())
      .get('/test')
      .set('x-request-id', 'custom-id-123');
    expect(res.headers['x-request-id']).toBe('custom-id-123');
    expect(res.body.requestId).toBe('custom-id-123');
  });

  test('generates UUID when no x-request-id provided', async () => {
    const res = await request(buildApp()).get('/test');
    expect(res.body.requestId).toMatch(/^[0-9a-f-]{36}$/);
  });
});
