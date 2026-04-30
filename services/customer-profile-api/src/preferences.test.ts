import request from 'supertest';
import { createApp } from './app';
import { CustomerStore } from './store';

describe('communication preferences', () => {
  let app: ReturnType<typeof createApp>;
  let customerId: string;

  beforeEach(async () => {
    app = createApp({ customers: new CustomerStore() });
    const res = await request(app)
      .post('/customers')
      .send({ firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com' });
    customerId = res.body.id;
  });

  test('PUT preferences persists the full payload', async () => {
    const prefs = {
      emailOptIn: false,
      smsOptIn: true,
      marketingOptIn: true,
      locale: 'fr-FR',
    };
    const res = await request(app)
      .put(`/customers/${customerId}/preferences`)
      .send(prefs);

    expect(res.status).toBe(200);
    expect(res.body.emailOptIn).toBe(false);
    expect(res.body.smsOptIn).toBe(true);
    expect(res.body.marketingOptIn).toBe(true);
    expect(res.body.locale).toBe('fr-FR');
    expect(res.body.customerId).toBe(customerId);
  });

  test('GET preferences returns defaults when none set', async () => {
    const res = await request(app).get(`/customers/${customerId}/preferences`);
    expect(res.status).toBe(200);
    expect(res.body.emailOptIn).toBe(true);
    expect(res.body.smsOptIn).toBe(false);
    expect(res.body.marketingOptIn).toBe(false);
    expect(res.body.locale).toBe('en-US');
  });

  test('GET preferences returns previously saved preferences', async () => {
    await request(app)
      .put(`/customers/${customerId}/preferences`)
      .send({ emailOptIn: false, smsOptIn: true, marketingOptIn: false, locale: 'de-DE' });

    const res = await request(app).get(`/customers/${customerId}/preferences`);
    expect(res.status).toBe(200);
    expect(res.body.smsOptIn).toBe(true);
    expect(res.body.locale).toBe('de-DE');
  });

  test('GET preferences for non-existent customer returns 404', async () => {
    const res = await request(app).get('/customers/cust_nonexistent/preferences');
    expect(res.status).toBe(404);
  });

  test('PUT preferences for non-existent customer returns 404', async () => {
    const res = await request(app)
      .put('/customers/cust_nonexistent/preferences')
      .send({ emailOptIn: true, smsOptIn: false, marketingOptIn: false, locale: 'en-US' });
    expect(res.status).toBe(404);
  });
});
