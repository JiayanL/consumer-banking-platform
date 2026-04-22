import request from 'supertest';
import { createApp } from './app';
import { NotificationStore, TemplateStore } from './store';

describe('POST /notify/email', () => {
  test('accepts a plain body and stores a record', async () => {
    const app = createApp({ notifications: new NotificationStore(), templates: new TemplateStore() });
    const res = await request(app)
      .post('/notify/email')
      .send({ to: 'user@example.com', subject: 'Hi {{name}}', body: 'Welcome {{name}}', vars: { name: 'Ada' } });

    expect(res.status).toBe(202);
    expect(res.body.channel).toBe('email');
    expect(res.body.to).toBe('user@example.com');
    expect(res.body.subject).toBe('Hi Ada');
    expect(res.body.body).toBe('Welcome Ada');
    expect(res.body.id).toMatch(/^ntf_/);
  });
});
