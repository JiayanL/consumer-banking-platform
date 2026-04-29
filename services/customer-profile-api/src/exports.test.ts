import assert from 'node:assert/strict';
import { customersToCsv, preferencesToCsv } from './exports';
import { Customer, CommunicationPreferences } from './types';

function makeCustomer(overrides: Partial<Customer> = {}): Customer {
  return {
    id: 'cust_001',
    firstName: 'Ada',
    lastName: 'Lovelace',
    email: 'ada@example.com',
    phone: '555-123-4567',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-02T00:00:00Z',
    ...overrides,
  };
}

function makePrefs(overrides: Partial<CommunicationPreferences> = {}): CommunicationPreferences {
  return {
    customerId: 'cust_001',
    emailOptIn: true,
    smsOptIn: false,
    marketingOptIn: true,
    locale: 'en-US',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('customersToCsv', () => {
  test('empty array produces header only', () => {
    const csv = customersToCsv([]);
    assert.strictEqual(csv, 'id,firstName,lastName,email,phone,createdAt');
  });

  test('single customer row', () => {
    const csv = customersToCsv([makeCustomer()]);
    const lines = csv.split('\n');
    assert.strictEqual(lines.length, 2);
    assert.strictEqual(lines[0], 'id,firstName,lastName,email,phone,createdAt');
    assert.strictEqual(
      lines[1],
      'cust_001,Ada,Lovelace,ada@example.com,555-123-4567,2024-01-01T00:00:00Z',
    );
  });

  test('multiple customers', () => {
    const customers = [
      makeCustomer({ id: 'cust_001' }),
      makeCustomer({ id: 'cust_002', firstName: 'Grace', lastName: 'Hopper', email: 'grace@example.com' }),
    ];
    const lines = customersToCsv(customers).split('\n');
    assert.strictEqual(lines.length, 3);
    assert.ok(lines[1].startsWith('cust_001,'));
    assert.ok(lines[2].startsWith('cust_002,'));
  });

  test('missing optional phone renders as empty', () => {
    const csv = customersToCsv([makeCustomer({ phone: undefined })]);
    const lines = csv.split('\n');
    // id,firstName,lastName,email,,createdAt  (phone column is empty)
    const cells = lines[1].split(',');
    assert.strictEqual(cells[4], '');
  });

  test('null-ish field values render as empty', () => {
    // Force a null value via cast to exercise the null branch
    const c = makeCustomer({ phone: null as unknown as string });
    const csv = customersToCsv([c]);
    const cells = csv.split('\n')[1].split(',');
    assert.strictEqual(cells[4], '');
  });

  test('field containing comma is quoted', () => {
    const csv = customersToCsv([makeCustomer({ lastName: 'Love,lace' })]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('"Love,lace"'));
  });

  test('field containing double-quote is escaped', () => {
    const csv = customersToCsv([makeCustomer({ firstName: 'A"da' })]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('"A""da"'));
  });

  test('field containing newline is quoted', () => {
    const csv = customersToCsv([makeCustomer({ firstName: 'A\nda' })]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('"A'));
    // The entire cell should be wrapped: "A\nda"
    const joined = csv.split('\n').slice(1).join('\n');
    assert.ok(joined.includes('"A\nda"'));
  });

  test('maskPII delegates to maskedView', () => {
    const c = makeCustomer({ email: 'ada@example.com', phone: '555-123-4567' });
    const csv = customersToCsv([c], { maskPII: true });
    const row = csv.split('\n')[1];
    // maskedView masks email: a*a@example.com (first char + stars + last char @ domain)
    assert.ok(!row.includes('ada@example.com'), 'email should be masked');
    // maskedView masks phone: ***-***-4567
    assert.ok(!row.includes('555-123-4567'), 'phone should be masked');
    assert.ok(row.includes('4567'), 'last 4 digits of phone should remain');
  });

  test('maskPII false leaves PII intact', () => {
    const c = makeCustomer();
    const csv = customersToCsv([c], { maskPII: false });
    const row = csv.split('\n')[1];
    assert.ok(row.includes('ada@example.com'));
    assert.ok(row.includes('555-123-4567'));
  });

  test('default opts (no maskPII) leaves PII intact', () => {
    const c = makeCustomer();
    const csv = customersToCsv([c]);
    const row = csv.split('\n')[1];
    assert.ok(row.includes('ada@example.com'));
  });
});

describe('preferencesToCsv', () => {
  test('empty array produces header only', () => {
    const csv = preferencesToCsv([]);
    assert.strictEqual(csv, 'customerId,emailOptIn,smsOptIn,marketingOptIn,locale,updatedAt');
  });

  test('single preferences row', () => {
    const csv = preferencesToCsv([makePrefs()]);
    const lines = csv.split('\n');
    assert.strictEqual(lines.length, 2);
    assert.strictEqual(lines[0], 'customerId,emailOptIn,smsOptIn,marketingOptIn,locale,updatedAt');
    assert.strictEqual(lines[1], 'cust_001,true,false,true,en-US,2024-01-01T00:00:00Z');
  });

  test('multiple preferences rows', () => {
    const prefs = [
      makePrefs({ customerId: 'cust_001' }),
      makePrefs({ customerId: 'cust_002', emailOptIn: false, smsOptIn: true }),
    ];
    const lines = preferencesToCsv(prefs).split('\n');
    assert.strictEqual(lines.length, 3);
    assert.ok(lines[1].startsWith('cust_001,'));
    assert.ok(lines[2].startsWith('cust_002,'));
    assert.ok(lines[2].includes('false,true,true'));
  });

  test('boolean fields render as true/false strings', () => {
    const csv = preferencesToCsv([makePrefs({ emailOptIn: false, smsOptIn: false, marketingOptIn: false })]);
    const row = csv.split('\n')[1];
    assert.strictEqual(row, 'cust_001,false,false,false,en-US,2024-01-01T00:00:00Z');
  });
});
