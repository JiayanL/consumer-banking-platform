import request from 'supertest';
import { createApp } from './app';

describe('api-gateway', () => {
  test('GET /healthz returns ok', async () => {
    const app = createApp();
    const res = await request(app).get('/healthz');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
    expect(res.headers['x-request-id']).toBeDefined();
  });

  test('/api/* without Authorization returns 401', async () => {
    const app = createApp();
    const res = await request(app).get('/api/accounts/acc_1');
    expect(res.status).toBe(401);
    expect(res.body.error).toMatch(/authorization/i);
  });
});
