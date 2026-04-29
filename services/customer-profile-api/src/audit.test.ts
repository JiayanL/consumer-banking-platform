import assert from 'node:assert/strict';

// audit.ts uses module-level state (ring buffer). We re-import per test
// suite via jest.isolateModules to get a clean ring for each describe block.

describe('audit', () => {
  let recordAudit: typeof import('./audit').recordAudit;
  let listAudit: typeof import('./audit').listAudit;

  beforeEach(() => {
    jest.isolateModules(() => {
      const mod = require('./audit');
      recordAudit = mod.recordAudit;
      listAudit = mod.listAudit;
    });
  });

  describe('recordAudit', () => {
    test('returns a record with generated id and recordedAt', () => {
      const r = recordAudit({ actor: 'admin', action: 'create', customerId: 'cust_1' });
      assert.match(r.id, /^aud_\d+$/);
      assert.ok(r.recordedAt);
      assert.strictEqual(r.actor, 'admin');
      assert.strictEqual(r.action, 'create');
      assert.strictEqual(r.customerId, 'cust_1');
    });

    test('assigns sequential ids', () => {
      const r1 = recordAudit({ actor: 'admin', action: 'create', customerId: 'cust_1' });
      const r2 = recordAudit({ actor: 'admin', action: 'update', customerId: 'cust_1' });
      assert.strictEqual(r1.id, 'aud_1');
      assert.strictEqual(r2.id, 'aud_2');
    });

    test('preserves optional metadata', () => {
      const r = recordAudit({
        actor: 'system',
        action: 'view',
        customerId: 'cust_1',
        metadata: { ip: '10.0.0.1' },
      });
      assert.deepStrictEqual(r.metadata, { ip: '10.0.0.1' });
    });

    test('ring buffer evicts oldest entry beyond RING_SIZE (512)', () => {
      for (let i = 0; i < 513; i++) {
        recordAudit({ actor: 'bot', action: 'view', customerId: 'cust_1' });
      }
      // The first entry (aud_1) should have been evicted
      const all = listAudit('cust_1');
      const ids = all.map((r) => r.id);
      assert.ok(!ids.includes('aud_1'), 'oldest entry should be evicted');
      assert.ok(ids.includes('aud_513'), 'newest entry should be present');
      assert.strictEqual(all.length, 512);
    });
  });

  describe('listAudit', () => {
    test('returns only records for the given customerId', () => {
      recordAudit({ actor: 'admin', action: 'create', customerId: 'cust_1' });
      recordAudit({ actor: 'admin', action: 'create', customerId: 'cust_2' });
      recordAudit({ actor: 'admin', action: 'update', customerId: 'cust_1' });

      const result = listAudit('cust_1');
      assert.strictEqual(result.length, 2);
      assert.ok(result.every((r) => r.customerId === 'cust_1'));
    });

    test('returns empty array for unknown customerId', () => {
      const result = listAudit('cust_unknown');
      assert.strictEqual(result.length, 0);
    });
  });
});
