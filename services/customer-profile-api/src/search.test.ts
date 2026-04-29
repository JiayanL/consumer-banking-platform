import assert from 'node:assert/strict';
import { searchCustomers, SearchQuery } from './search';
import { Customer } from './types';

function makeCustomer(overrides: Partial<Customer> = {}): Customer {
  return {
    id: 'cust_1',
    firstName: 'Ada',
    lastName: 'Lovelace',
    email: 'ada@example.com',
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

const customers: Customer[] = [
  makeCustomer({ id: 'cust_1', firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com', phone: '+15551111111' }),
  makeCustomer({ id: 'cust_2', firstName: 'Charles', lastName: 'Babbage', email: 'charles@corp.io' }),
  makeCustomer({ id: 'cust_3', firstName: 'Grace', lastName: 'Hopper', email: 'grace@example.com', phone: '+15552222222' }),
];

describe('searchCustomers', () => {
  test('returns all customers with empty query', () => {
    const result = searchCustomers(customers, {});
    assert.strictEqual(result.total, 3);
    assert.strictEqual(result.items.length, 3);
  });

  test('applies default limit of 50 and offset of 0', () => {
    const result = searchCustomers(customers, {});
    assert.strictEqual(result.limit, 50);
    assert.strictEqual(result.offset, 0);
  });

  test('filters by free-text query (first name)', () => {
    const result = searchCustomers(customers, { q: 'ada' });
    assert.strictEqual(result.total, 1);
    assert.strictEqual(result.items[0].id, 'cust_1');
  });

  test('free-text query is case-insensitive', () => {
    const result = searchCustomers(customers, { q: 'BABBAGE' });
    assert.strictEqual(result.total, 1);
    assert.strictEqual(result.items[0].id, 'cust_2');
  });

  test('free-text query matches against email', () => {
    const result = searchCustomers(customers, { q: 'corp.io' });
    assert.strictEqual(result.total, 1);
    assert.strictEqual(result.items[0].id, 'cust_2');
  });

  test('free-text query trims whitespace', () => {
    const result = searchCustomers(customers, { q: '  grace  ' });
    assert.strictEqual(result.total, 1);
    assert.strictEqual(result.items[0].id, 'cust_3');
  });

  test('filters by email domain', () => {
    const result = searchCustomers(customers, { emailDomain: 'example.com' });
    assert.strictEqual(result.total, 2);
    const ids = result.items.map((c) => c.id);
    assert.ok(ids.includes('cust_1'));
    assert.ok(ids.includes('cust_3'));
  });

  test('emailDomain filter is case-insensitive', () => {
    const result = searchCustomers(customers, { emailDomain: 'CORP.IO' });
    assert.strictEqual(result.total, 1);
  });

  test('filters by hasPhone=true', () => {
    const result = searchCustomers(customers, { hasPhone: true });
    assert.strictEqual(result.total, 2);
  });

  test('filters by hasPhone=false', () => {
    const result = searchCustomers(customers, { hasPhone: false });
    assert.strictEqual(result.total, 1);
    assert.strictEqual(result.items[0].id, 'cust_2');
  });

  test('combines multiple filters', () => {
    const result = searchCustomers(customers, { q: 'example', hasPhone: true });
    assert.strictEqual(result.total, 2);
  });

  test('respects limit parameter', () => {
    const result = searchCustomers(customers, { limit: 2 });
    assert.strictEqual(result.items.length, 2);
    assert.strictEqual(result.total, 3);
    assert.strictEqual(result.limit, 2);
  });

  test('respects offset parameter', () => {
    const result = searchCustomers(customers, { offset: 2 });
    assert.strictEqual(result.items.length, 1);
    assert.strictEqual(result.offset, 2);
  });

  test('clamps limit to minimum of 1', () => {
    const result = searchCustomers(customers, { limit: 0 });
    assert.strictEqual(result.limit, 1);
  });

  test('clamps limit to maximum of 500', () => {
    const result = searchCustomers(customers, { limit: 9999 });
    assert.strictEqual(result.limit, 500);
  });

  test('clamps negative offset to 0', () => {
    const result = searchCustomers(customers, { offset: -5 });
    assert.strictEqual(result.offset, 0);
  });

  test('returns empty items when no matches', () => {
    const result = searchCustomers(customers, { q: 'nonexistent' });
    assert.strictEqual(result.total, 0);
    assert.strictEqual(result.items.length, 0);
  });
});
