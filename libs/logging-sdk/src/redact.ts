/**
 * Best-effort PII redaction for log payloads.
 *
 * @compliance-critical PII_HANDLING
 */
const SSN_RE = /\b\d{3}-\d{2}-\d{4}\b/g;
// 16-digit PAN (primary account number). We don't luhn-check here.
const PAN_RE = /\b(?:\d[ -]?){13,19}\b/g;

export function redactPII<T>(value: T): T {
  return _redact(value) as T;
}

function _redact(v: unknown): unknown {
  if (v == null) return v;
  if (typeof v === 'string') return redactString(v);
  if (Array.isArray(v)) return v.map(_redact);
  if (typeof v === 'object') {
    const out: Record<string, unknown> = {};
    for (const [k, val] of Object.entries(v as Record<string, unknown>)) {
      out[k] = _redact(val);
    }
    return out;
  }
  return v;
}

function redactString(s: string): string {
  return s.replace(SSN_RE, '***-**-****').replace(PAN_RE, (match) => {
    const digits = match.replace(/[^0-9]/g, '');
    if (digits.length < 13) return match;
    return '*'.repeat(digits.length - 4) + digits.slice(-4);
  });
}
