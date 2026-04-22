import assert from 'node:assert/strict';
import request from 'supertest';
import { createApp } from './app';
import { CustomerStore } from './store';

describe('customers', () => {
  test('create + get happy path', async () => {
    const app = createApp({ customers: new CustomerStore() });

    const created = await request(app)
      .post('/customers')
      .send({ firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com' });

    assert.strictEqual(created.status, 201);
    assert.strictEqual(created.body.firstName, 'Ada');
    assert.strictEqual(created.body.lastName, 'Lovelace');
    assert.strictEqual(created.body.email, 'ada@example.com');
    assert.match(created.body.id, /^cust_/);

    const fetched = await request(app).get(`/customers/${created.body.id}`);
    assert.strictEqual(fetched.status, 200);
    assert.deepStrictEqual(fetched.body, created.body);
  });
});
