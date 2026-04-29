import assert from 'node:assert/strict';
import type { AuditRecord } from './audit';

jest.mock('./logger', () => ({
  logger: { debug: jest.fn() },
}));

describe('recordAudit', () => {
  let recordAudit: typeof import('./audit').recordAudit;
  let listAudit: typeof import('./audit').listAudit;
  let mockDebug: jest.Mock;

  beforeEach(() => {
    jest.isolateModules(() => {
      const { logger } = require('./logger');
      mockDebug = logger.debug;
      mockDebug.mockClear();
      const mod = require('./audit');
      recordAudit = mod.recordAudit;
      listAudit = mod.listAudit;
    });
  });

  test('assigns sequential ids starting at aud_1', () => {
    const r1 = recordAudit({ actor: 'u1', action: 'create', customerId: 'c1' });
    const r2 = recordAudit({ actor: 'u1', action: 'view', customerId: 'c1' });

    assert.equal(r1.id, 'aud_1');
    assert.equal(r2.id, 'aud_2');
  });

  test('sets an ISO-8601 recordedAt timestamp', () => {
    const before = new Date().toISOString();
    const r = recordAudit({ actor: 'u1', action: 'update', customerId: 'c1' });
    const after = new Date().toISOString();

    assert.equal(typeof r.recordedAt, 'string');
    assert.ok(r.recordedAt >= before && r.recordedAt <= after);
  });

  test('preserves actor, action, and customerId from input', () => {
    const r = recordAudit({ actor: 'u42', action: 'preferences.update', customerId: 'c99' });

    assert.equal(r.actor, 'u42');
    assert.equal(r.action, 'preferences.update');
    assert.equal(r.customerId, 'c99');
  });

  test('preserves optional metadata', () => {
    const meta = { ip: '10.0.0.1', reason: 'address change' };
    const r = recordAudit({ actor: 'u1', action: 'update', customerId: 'c1', metadata: meta });

    assert.deepEqual(r.metadata, meta);
  });

  test('returns record without metadata when not provided', () => {
    const r = recordAudit({ actor: 'u1', action: 'view', customerId: 'c1' });

    assert.equal(r.metadata, undefined);
  });

  test('calls logger.debug with id and action', () => {
    const r = recordAudit({ actor: 'u1', action: 'create', customerId: 'c1' });

    assert.equal(mockDebug.mock.calls.length, 1);
    assert.equal(mockDebug.mock.calls[0][0], 'audit.appended');
    assert.deepEqual(mockDebug.mock.calls[0][1], { id: r.id, action: 'create' });
  });

  test('evicts oldest entry when ring exceeds 512', () => {
    for (let i = 0; i < 513; i++) {
      recordAudit({ actor: 'u1', action: 'view', customerId: 'c1' });
    }

    const all = listAudit('c1');
    assert.equal(all.length, 512);
    assert.equal(all[0].id, 'aud_2');
    assert.equal(all[all.length - 1].id, 'aud_513');
  });

  test('ring holds exactly 512 entries at capacity', () => {
    for (let i = 0; i < 512; i++) {
      recordAudit({ actor: 'u1', action: 'view', customerId: 'c1' });
    }

    const all = listAudit('c1');
    assert.equal(all.length, 512);
    assert.equal(all[0].id, 'aud_1');
    assert.equal(all[511].id, 'aud_512');
  });
});

describe('listAudit', () => {
  let recordAudit: typeof import('./audit').recordAudit;
  let listAudit: typeof import('./audit').listAudit;

  beforeEach(() => {
    jest.isolateModules(() => {
      const mod = require('./audit');
      recordAudit = mod.recordAudit;
      listAudit = mod.listAudit;
    });
  });

  test('returns empty array when ring is empty', () => {
    assert.deepEqual(listAudit('c1'), []);
  });

  test('filters records by customerId', () => {
    recordAudit({ actor: 'u1', action: 'create', customerId: 'c1' });
    recordAudit({ actor: 'u2', action: 'view', customerId: 'c2' });
    recordAudit({ actor: 'u1', action: 'update', customerId: 'c1' });

    const c1Records = listAudit('c1');
    assert.equal(c1Records.length, 2);
    assert.ok(c1Records.every((r: AuditRecord) => r.customerId === 'c1'));
  });

  test('returns empty array for unknown customerId', () => {
    recordAudit({ actor: 'u1', action: 'create', customerId: 'c1' });

    assert.deepEqual(listAudit('nonexistent'), []);
  });

  test('returns records in insertion order', () => {
    recordAudit({ actor: 'u1', action: 'create', customerId: 'c1' });
    recordAudit({ actor: 'u1', action: 'view', customerId: 'c1' });
    recordAudit({ actor: 'u1', action: 'update', customerId: 'c1' });

    const records = listAudit('c1');
    assert.equal(records[0].action, 'create');
    assert.equal(records[1].action, 'view');
    assert.equal(records[2].action, 'update');
  });
});
