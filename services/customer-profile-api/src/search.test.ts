import { searchCustomers } from './search';
import { Customer } from './types';

const CUSTOMERS: Customer[] = [
  { id: 'c1', firstName: 'Alice', lastName: 'Smith', email: 'alice@example.com', phone: '+14155551234', createdAt: '2024-01-01', updatedAt: '2024-01-01' },
  { id: 'c2', firstName: 'Bob', lastName: 'Jones', email: 'bob@acme.com', createdAt: '2024-01-02', updatedAt: '2024-01-02' },
  { id: 'c3', firstName: 'Carol', lastName: 'White', email: 'carol@example.com', phone: '+14155559999', createdAt: '2024-01-03', updatedAt: '2024-01-03' },
];

describe('searchCustomers', () => {
  test('no filters returns all', () => {
    const result = searchCustomers(CUSTOMERS, {});
    expect(result.items).toHaveLength(3);
    expect(result.total).toBe(3);
  });

  test('text search filters by name', () => {
    const result = searchCustomers(CUSTOMERS, { q: 'Alice' });
    expect(result.items).toHaveLength(1);
    expect(result.items[0].id).toBe('c1');
  });

  test('text search filters by email', () => {
    const result = searchCustomers(CUSTOMERS, { q: 'bob@acme' });
    expect(result.items).toHaveLength(1);
    expect(result.items[0].id).toBe('c2');
  });

  test('text search is case-insensitive', () => {
    const result = searchCustomers(CUSTOMERS, { q: 'alice' });
    expect(result.items).toHaveLength(1);
  });

  test('emailDomain filter', () => {
    const result = searchCustomers(CUSTOMERS, { emailDomain: 'acme.com' });
    expect(result.items).toHaveLength(1);
    expect(result.items[0].id).toBe('c2');
  });

  test('hasPhone filter true', () => {
    const result = searchCustomers(CUSTOMERS, { hasPhone: true });
    expect(result.items).toHaveLength(2);
  });

  test('hasPhone filter false', () => {
    const result = searchCustomers(CUSTOMERS, { hasPhone: false });
    expect(result.items).toHaveLength(1);
    expect(result.items[0].id).toBe('c2');
  });

  test('pagination with limit', () => {
    const result = searchCustomers(CUSTOMERS, { limit: 2 });
    expect(result.items).toHaveLength(2);
    expect(result.limit).toBe(2);
  });

  test('pagination with offset', () => {
    const result = searchCustomers(CUSTOMERS, { offset: 2 });
    expect(result.items).toHaveLength(1);
    expect(result.offset).toBe(2);
  });

  test('limit clamped at 500', () => {
    const result = searchCustomers(CUSTOMERS, { limit: 1000 });
    expect(result.limit).toBe(500);
  });

  test('limit minimum is 1', () => {
    const result = searchCustomers(CUSTOMERS, { limit: -5 });
    expect(result.limit).toBe(1);
  });
});
