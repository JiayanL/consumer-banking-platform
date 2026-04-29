import assert from 'node:assert/strict';
import { customersToCsv, preferencesToCsv } from './exports';
import { Customer, CommunicationPreferences } from './types';

const BASE_CUSTOMER: Customer = {
  id: 'cust_001',
  firstName: 'Ada',
  lastName: 'Lovelace',
  email: 'ada@example.com',
  phone: '555-867-5309',
  createdAt: '2024-01-15T00:00:00Z',
  updatedAt: '2024-06-01T00:00:00Z',
};

describe('customersToCsv', () => {
  test('returns only header row for empty array', () => {
    const csv = customersToCsv([]);
    assert.strictEqual(csv, 'id,firstName,lastName,email,phone,createdAt');
  });

  test('produces header + one data row for a single customer', () => {
    const csv = customersToCsv([BASE_CUSTOMER]);
    const lines = csv.split('\n');
    assert.strictEqual(lines.length, 2);
    assert.strictEqual(lines[0], 'id,firstName,lastName,email,phone,createdAt');
    assert.strictEqual(
      lines[1],
      'cust_001,Ada,Lovelace,ada@example.com,555-867-5309,2024-01-15T00:00:00Z',
    );
  });

  test('renders multiple customers as separate rows', () => {
    const second: Customer = {
      ...BASE_CUSTOMER,
      id: 'cust_002',
      firstName: 'Grace',
      lastName: 'Hopper',
      email: 'grace@example.com',
    };
    const csv = customersToCsv([BASE_CUSTOMER, second]);
    const lines = csv.split('\n');
    assert.strictEqual(lines.length, 3);
    assert.ok(lines[1].startsWith('cust_001'));
    assert.ok(lines[2].startsWith('cust_002'));
  });

  test('handles missing optional phone field', () => {
    const noPhone: Customer = { ...BASE_CUSTOMER, phone: undefined };
    const csv = customersToCsv([noPhone]);
    const row = csv.split('\n')[1];
    const cells = row.split(',');
    assert.strictEqual(cells[4], '');
  });

  test('escapes values containing commas', () => {
    const c: Customer = { ...BASE_CUSTOMER, lastName: 'Van Rossum, Jr.' };
    const csv = customersToCsv([c]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('"Van Rossum, Jr."'));
  });

  test('escapes values containing double quotes', () => {
    const c: Customer = { ...BASE_CUSTOMER, firstName: 'The "Great"' };
    const csv = customersToCsv([c]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('"The ""Great"""'));
  });

  test('escapes values containing newlines', () => {
    const c: Customer = { ...BASE_CUSTOMER, firstName: 'Line1\nLine2' };
    const csv = customersToCsv([c]);
    assert.ok(csv.includes('"Line1\nLine2"'));
  });

  test('masks PII when opts.maskPII is true', () => {
    const csv = customersToCsv([BASE_CUSTOMER], { maskPII: true });
    const row = csv.split('\n')[1];
    assert.ok(!row.includes('ada@example.com'), 'email should be masked');
    assert.ok(!row.includes('555-867-5309'), 'phone should be masked');
    assert.ok(row.includes('cust_001'), 'id should remain unmasked');
    assert.ok(row.includes('Ada'), 'firstName should remain unmasked');
  });

  test('does not mask PII when opts.maskPII is false', () => {
    const csv = customersToCsv([BASE_CUSTOMER], { maskPII: false });
    const row = csv.split('\n')[1];
    assert.ok(row.includes('ada@example.com'));
    assert.ok(row.includes('555-867-5309'));
  });

  test('does not mask PII when opts is omitted', () => {
    const csv = customersToCsv([BASE_CUSTOMER]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('ada@example.com'));
  });
});

describe('preferencesToCsv', () => {
  const PREFS: CommunicationPreferences = {
    customerId: 'cust_001',
    emailOptIn: true,
    smsOptIn: false,
    marketingOptIn: true,
    locale: 'en-US',
    updatedAt: '2024-06-01T00:00:00Z',
  };

  test('returns only header row for empty array', () => {
    const csv = preferencesToCsv([]);
    assert.strictEqual(
      csv,
      'customerId,emailOptIn,smsOptIn,marketingOptIn,locale,updatedAt',
    );
  });

  test('renders boolean values as true/false strings', () => {
    const csv = preferencesToCsv([PREFS]);
    const row = csv.split('\n')[1];
    assert.strictEqual(
      row,
      'cust_001,true,false,true,en-US,2024-06-01T00:00:00Z',
    );
  });

  test('renders multiple preference rows', () => {
    const second: CommunicationPreferences = {
      ...PREFS,
      customerId: 'cust_002',
      locale: 'fr-FR',
    };
    const csv = preferencesToCsv([PREFS, second]);
    const lines = csv.split('\n');
    assert.strictEqual(lines.length, 3);
    assert.ok(lines[2].includes('fr-FR'));
  });
});
