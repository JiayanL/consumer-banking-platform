import { Customer } from './types';

/**
 * Utility helpers for producing masked views of customer PII. These
 * are used by the admin console and by audit log renderers. Kept out
 * of the request path but next to the profile data so the review
 * surface stays together.
 */
export function maskEmail(email: string): string {
  const [user, domain] = email.split('@');
  if (!domain) return email;
  if (user.length <= 2) return `*@${domain}`;
  return `${user[0]}${'*'.repeat(user.length - 2)}${user[user.length - 1]}@${domain}`;
}

export function maskPhone(phone: string): string {
  const digits = phone.replace(/\D/g, '');
  if (digits.length < 4) return phone;
  return `***-***-${digits.slice(-4)}`;
}

export function maskedView(c: Customer): Customer {
  return {
    ...c,
    email: maskEmail(c.email),
    phone: c.phone ? maskPhone(c.phone) : undefined,
  };
}

export function initials(c: Customer): string {
  const f = c.firstName?.[0] ?? '';
  const l = c.lastName?.[0] ?? '';
  return `${f}${l}`.toUpperCase();
}
