import { Customer, CommunicationPreferences } from './types';
import { maskedView } from './masking';

/**
 * CSV export helpers for the admin console. Deliberately tolerant of
 * missing fields — the admin console will render a "—" for blanks.
 */
const CSV_COLUMNS = ['id', 'firstName', 'lastName', 'email', 'phone', 'createdAt'] as const;

export function customersToCsv(customers: Customer[], opts: { maskPII?: boolean } = {}): string {
  const rows = customers.map((c) => (opts.maskPII ? maskedView(c) : c));
  const lines: string[] = [];
  lines.push(CSV_COLUMNS.join(','));
  for (const row of rows) {
    const cells = CSV_COLUMNS.map((col) => {
      const v = (row as unknown as Record<string, unknown>)[col];
      if (v === undefined || v === null) return '';
      const s = String(v);
      if (s.includes(',') || s.includes('"') || s.includes('\n')) {
        return `"${s.replace(/"/g, '""')}"`;
      }
      return s;
    });
    lines.push(cells.join(','));
  }
  return lines.join('\n');
}

export function preferencesToCsv(prefs: CommunicationPreferences[]): string {
  const header = ['customerId', 'emailOptIn', 'smsOptIn', 'marketingOptIn', 'locale', 'updatedAt'];
  const lines = [header.join(',')];
  for (const p of prefs) {
    lines.push([p.customerId, p.emailOptIn, p.smsOptIn, p.marketingOptIn, p.locale, p.updatedAt].join(','));
  }
  return lines.join('\n');
}


