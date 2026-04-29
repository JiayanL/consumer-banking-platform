import assert from 'node:assert/strict';
import { searchCustomers, SearchQuery, SearchResult } from './search';
import { Customer } from './types';

function makeCustomer(overrides: Partial<Customer> = {}): Customer {
  return {
    id: 'cust_001',
    firstName: 'Ada',
    lastName: 'Lovelace',
    email: 'ada@example.com',
    phone: '+1-555-0100',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  };
}

const FIXTURES: Customer[] = [
  makeCustomer({ id: 'cust_001', firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com', phone: '+1-555-0100' }),
  makeCustomer({ id: 'cust_002', firstName: 'Grace', lastName: 'Hopper', email: 'grace@navy.mil', phone: undefined }),
  makeCustomer({ id: 'cust_003', firstName: 'Alan', lastName: 'Turing', email: 'alan@example.com', phone: '+44-20-7946-0958' }),
  makeCustomer({ id: 'cust_004', firstName: 'Barbara', lastName: 'Liskov', email: 'barbara@mit.edu', phone: undefined }),
  makeCustomer({ id: 'cust_005', firstName: 'Linus', lastName: 'Torvalds', email: 'linus@example.com', phone: '+358-9-123-4567' }),
];

describe('searchCustomers', () => {
  describe('no filters', () => {
    test('returns all customers when query is empty', () => {
      const result = searchCustomers(FIXTURES, {});
      assert.strictEqual(result.total, 5);
      assert.strictEqual(result.items.length, 5);
    });

    test('returns empty result for empty input array', () => {
      const result = searchCustomers([], {});
      assert.strictEqual(result.total, 0);
      assert.strictEqual(result.items.length, 0);
    });
  });

  describe('q (free-text) filter', () => {
    test('matches on firstName', () => {
      const result = searchCustomers(FIXTURES, { q: 'Ada' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_001');
    });

    test('matches on lastName', () => {
      const result = searchCustomers(FIXTURES, { q: 'Hopper' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_002');
    });

    test('matches on email', () => {
      const result = searchCustomers(FIXTURES, { q: 'navy.mil' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_002');
    });

    test('is case-insensitive', () => {
      const result = searchCustomers(FIXTURES, { q: 'ADA' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_001');
    });

    test('trims whitespace from query', () => {
      const result = searchCustomers(FIXTURES, { q: '  Turing  ' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_003');
    });

    test('matches partial strings', () => {
      const result = searchCustomers(FIXTURES, { q: 'lov' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_001');
    });

    test('returns multiple matches', () => {
      const result = searchCustomers(FIXTURES, { q: 'example.com' });
      assert.strictEqual(result.total, 3);
      const ids = result.items.map((c) => c.id);
      assert.deepStrictEqual(ids, ['cust_001', 'cust_003', 'cust_005']);
    });

    test('returns empty when nothing matches', () => {
      const result = searchCustomers(FIXTURES, { q: 'nonexistent' });
      assert.strictEqual(result.total, 0);
      assert.strictEqual(result.items.length, 0);
    });
  });

  describe('emailDomain filter', () => {
    test('filters by email domain', () => {
      const result = searchCustomers(FIXTURES, { emailDomain: 'example.com' });
      assert.strictEqual(result.total, 3);
      const ids = result.items.map((c) => c.id);
      assert.deepStrictEqual(ids, ['cust_001', 'cust_003', 'cust_005']);
    });

    test('is case-insensitive', () => {
      const result = searchCustomers(FIXTURES, { emailDomain: 'NAVY.MIL' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_002');
    });

    test('returns empty for unmatched domain', () => {
      const result = searchCustomers(FIXTURES, { emailDomain: 'yahoo.com' });
      assert.strictEqual(result.total, 0);
    });
  });

  describe('hasPhone filter', () => {
    test('filters customers with phone numbers', () => {
      const result = searchCustomers(FIXTURES, { hasPhone: true });
      assert.strictEqual(result.total, 3);
      const ids = result.items.map((c) => c.id);
      assert.deepStrictEqual(ids, ['cust_001', 'cust_003', 'cust_005']);
    });

    test('filters customers without phone numbers', () => {
      const result = searchCustomers(FIXTURES, { hasPhone: false });
      assert.strictEqual(result.total, 2);
      const ids = result.items.map((c) => c.id);
      assert.deepStrictEqual(ids, ['cust_002', 'cust_004']);
    });

    test('is not applied when undefined', () => {
      const result = searchCustomers(FIXTURES, { hasPhone: undefined });
      assert.strictEqual(result.total, 5);
    });
  });

  describe('combined filters', () => {
    test('q + emailDomain narrows results', () => {
      const result = searchCustomers(FIXTURES, { q: 'ada', emailDomain: 'example.com' });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_001');
    });

    test('q + hasPhone narrows results', () => {
      const result = searchCustomers(FIXTURES, { q: 'example.com', hasPhone: false });
      assert.strictEqual(result.total, 0);
    });

    test('emailDomain + hasPhone', () => {
      const result = searchCustomers(FIXTURES, { emailDomain: 'example.com', hasPhone: true });
      assert.strictEqual(result.total, 3);
      const ids = result.items.map((c) => c.id);
      assert.deepStrictEqual(ids, ['cust_001', 'cust_003', 'cust_005']);
    });

    test('all three filters combined', () => {
      const result = searchCustomers(FIXTURES, { q: 'linus', emailDomain: 'example.com', hasPhone: true });
      assert.strictEqual(result.total, 1);
      assert.strictEqual(result.items[0].id, 'cust_005');
    });
  });

  describe('limit (exercises clamp)', () => {
    test('defaults to 50 when not specified', () => {
      const result = searchCustomers(FIXTURES, {});
      assert.strictEqual(result.limit, 50);
    });

    test('respects a custom limit', () => {
      const result = searchCustomers(FIXTURES, { limit: 2 });
      assert.strictEqual(result.limit, 2);
      assert.strictEqual(result.items.length, 2);
      assert.strictEqual(result.total, 5);
    });

    test('clamps limit below 1 to 1', () => {
      const result = searchCustomers(FIXTURES, { limit: 0 });
      assert.strictEqual(result.limit, 1);
      assert.strictEqual(result.items.length, 1);
    });

    test('clamps negative limit to 1', () => {
      const result = searchCustomers(FIXTURES, { limit: -10 });
      assert.strictEqual(result.limit, 1);
    });

    test('clamps limit above 500 to 500', () => {
      const result = searchCustomers(FIXTURES, { limit: 999 });
      assert.strictEqual(result.limit, 500);
    });

    test('accepts limit at boundary 1', () => {
      const result = searchCustomers(FIXTURES, { limit: 1 });
      assert.strictEqual(result.limit, 1);
      assert.strictEqual(result.items.length, 1);
    });

    test('accepts limit at boundary 500', () => {
      const result = searchCustomers(FIXTURES, { limit: 500 });
      assert.strictEqual(result.limit, 500);
    });
  });

  describe('offset', () => {
    test('defaults to 0 when not specified', () => {
      const result = searchCustomers(FIXTURES, {});
      assert.strictEqual(result.offset, 0);
    });

    test('skips records with a positive offset', () => {
      const result = searchCustomers(FIXTURES, { offset: 2 });
      assert.strictEqual(result.offset, 2);
      assert.strictEqual(result.items.length, 3);
      assert.strictEqual(result.items[0].id, 'cust_003');
    });

    test('clamps negative offset to 0', () => {
      const result = searchCustomers(FIXTURES, { offset: -5 });
      assert.strictEqual(result.offset, 0);
    });

    test('returns empty items when offset exceeds total', () => {
      const result = searchCustomers(FIXTURES, { offset: 100 });
      assert.strictEqual(result.total, 5);
      assert.strictEqual(result.items.length, 0);
    });
  });

  describe('pagination (limit + offset together)', () => {
    test('pages through results correctly', () => {
      const page1 = searchCustomers(FIXTURES, { limit: 2, offset: 0 });
      assert.strictEqual(page1.items.length, 2);
      assert.strictEqual(page1.items[0].id, 'cust_001');
      assert.strictEqual(page1.items[1].id, 'cust_002');

      const page2 = searchCustomers(FIXTURES, { limit: 2, offset: 2 });
      assert.strictEqual(page2.items.length, 2);
      assert.strictEqual(page2.items[0].id, 'cust_003');
      assert.strictEqual(page2.items[1].id, 'cust_004');

      const page3 = searchCustomers(FIXTURES, { limit: 2, offset: 4 });
      assert.strictEqual(page3.items.length, 1);
      assert.strictEqual(page3.items[0].id, 'cust_005');
    });

    test('total reflects filtered count, not page size', () => {
      const result = searchCustomers(FIXTURES, { q: 'example.com', limit: 1, offset: 0 });
      assert.strictEqual(result.total, 3);
      assert.strictEqual(result.items.length, 1);
    });
  });

  describe('SearchResult shape', () => {
    test('returns all expected fields', () => {
      const result: SearchResult = searchCustomers(FIXTURES, { limit: 10, offset: 1 });
      assert.ok(Array.isArray(result.items));
      assert.strictEqual(typeof result.total, 'number');
      assert.strictEqual(typeof result.limit, 'number');
      assert.strictEqual(typeof result.offset, 'number');
    });

    test('items are the same object references from input', () => {
      const result = searchCustomers(FIXTURES, { limit: 1 });
      assert.strictEqual(result.items[0], FIXTURES[0]);
    });
  });
});
