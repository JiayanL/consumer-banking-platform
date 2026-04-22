const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const E164_RE = /^\+?[1-9]\d{6,14}$/;

export function isEmail(s: unknown): s is string {
  return typeof s === 'string' && EMAIL_RE.test(s);
}

export function isPhone(s: unknown): s is string {
  return typeof s === 'string' && E164_RE.test(s);
}

export function requireString(v: unknown, field: string): string {
  if (typeof v !== 'string' || v.length === 0) {
    throw new ValidationError(`${field} is required`);
  }
  return v;
}

export function requireBoolean(v: unknown, field: string): boolean {
  if (typeof v !== 'boolean') {
    throw new ValidationError(`${field} must be a boolean`);
  }
  return v;
}

export class ValidationError extends Error {
  constructor(msg: string) {
    super(msg);
    this.name = 'ValidationError';
  }
}
