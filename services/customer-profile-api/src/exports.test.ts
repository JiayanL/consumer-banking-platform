import assert from 'node:assert/strict';
import { customersToCsv, preferencesToCsv } from './exports';
import { Customer, CommunicationPreferences } from './types';

function makeCustomer(overrides: Partial<Customer> = {}): Customer {
  return {
    id: 'cust_1',
    firstName: 'Ada',
    lastName: 'Lovelace',
    email: 'ada@example.com',
    phone: '+15551234567',
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

describe('customersToCsv', () => {
  test('produces header + one row for a single customer', () => {
    const csv = customersToCsv([makeCustomer()]);
    const lines = csv.split('\n');
    assert.strictEqual(lines[0], 'id,firstName,lastName,email,phone,createdAt');
    assert.strictEqual(
      lines[1],
      'cust_1,Ada,Lovelace,ada@example.com,+15551234567,2025-01-01T00:00:00.000Z',
    );
  });

  test('returns header-only for empty array', () => {
    const csv = customersToCsv([]);
    assert.strictEqual(csv, 'id,firstName,lastName,email,phone,createdAt');
  });

  test('renders empty cell for undefined phone', () => {
    const csv = customersToCsv([makeCustomer({ phone: undefined })]);
    const row = csv.split('\n')[1];
    // phone column should be empty between the two commas
    assert.strictEqual(
      row,
      'cust_1,Ada,Lovelace,ada@example.com,,2025-01-01T00:00:00.000Z',
    );
  });

  test('quotes fields containing commas', () => {
    const csv = customersToCsv([makeCustomer({ lastName: 'O,Brien' })]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('"O,Brien"'));
  });

  test('escapes double-quotes inside a field', () => {
    const csv = customersToCsv([makeCustomer({ firstName: 'Say "Hi"' })]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('"Say ""Hi"""'));
  });

  test('quotes fields containing newlines', () => {
    const csv = customersToCsv([makeCustomer({ firstName: 'Line\nTwo' })]);
    const row = csv.split('\n')[1];
    assert.ok(row.startsWith('cust_1,"Line'));
  });

  test('masks PII when maskPII option is set', () => {
    const csv = customersToCsv([makeCustomer()], { maskPII: true });
    const row = csv.split('\n')[1];
    // email should be masked (a*a@example.com pattern)
    assert.ok(!row.includes('ada@example.com'), 'email should be masked');
    // phone should be masked (***-***-4567 pattern)
    assert.ok(!row.includes('+15551234567'), 'phone should be masked');
  });

  test('multiple customers produce multiple rows', () => {
    const customers = [
      makeCustomer({ id: 'cust_1' }),
      makeCustomer({ id: 'cust_2', firstName: 'Charles', lastName: 'Babbage', email: 'cb@example.com' }),
    ];
    const lines = customersToCsv(customers).split('\n');
    assert.strictEqual(lines.length, 3); // header + 2 rows
  });
});

describe('preferencesToCsv', () => {
  const prefs: CommunicationPreferences = {
    customerId: 'cust_1',
    emailOptIn: true,
    smsOptIn: false,
    marketingOptIn: true,
    locale: 'en-US',
    updatedAt: '2025-06-01T00:00:00.000Z',
  };

  test('produces header + one row', () => {
    const csv = preferencesToCsv([prefs]);
    const lines = csv.split('\n');
    assert.strictEqual(lines[0], 'customerId,emailOptIn,smsOptIn,marketingOptIn,locale,updatedAt');
    assert.strictEqual(lines[1], 'cust_1,true,false,true,en-US,2025-06-01T00:00:00.000Z');
  });

  test('returns header-only for empty array', () => {
    const csv = preferencesToCsv([]);
    const lines = csv.split('\n');
    assert.strictEqual(lines.length, 1);
  });
});
