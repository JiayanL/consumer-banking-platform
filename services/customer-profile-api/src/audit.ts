import { logger } from './logger';

export type AuditAction = 'create' | 'update' | 'view' | 'preferences.update';

export interface AuditRecord {
  id: string;
  actor: string;
  action: AuditAction;
  customerId: string;
  recordedAt: string;
  metadata?: Record<string, unknown>;
}

const ring: AuditRecord[] = [];
const RING_SIZE = 512;

/**
 * Append-only audit trail with an in-memory ring buffer. In production
 * this would ship to an append-only S3 bucket; here the ring is fine
 * for reproducing behaviour in tests and local dev.
 */
export function recordAudit(record: Omit<AuditRecord, 'id' | 'recordedAt'>): AuditRecord {
  const r: AuditRecord = {
    ...record,
    id: `aud_${ring.length + 1}`,
    recordedAt: new Date().toISOString(),
  };
  ring.push(r);
  if (ring.length > RING_SIZE) ring.shift();
  logger.debug('audit.appended', { id: r.id, action: r.action });
  return r;
}

export function listAudit(customerId: string): AuditRecord[] {
  return ring.filter((r) => r.customerId === customerId);
}
