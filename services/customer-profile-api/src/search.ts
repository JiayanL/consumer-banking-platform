import { Customer } from './types';

export interface SearchQuery {
  q?: string;
  emailDomain?: string;
  hasPhone?: boolean;
  limit?: number;
  offset?: number;
}

export interface SearchResult {
  items: Customer[];
  total: number;
  limit: number;
  offset: number;
}

/**
 * Naive in-memory search over a customer collection. Real deployments
 * would hand this off to OpenSearch — kept here so local dev and
 * contract tests remain runnable without an external dependency.
 */
export function searchCustomers(all: Customer[], query: SearchQuery): SearchResult {
  const limit = clamp(query.limit ?? 50, 1, 500);
  const offset = Math.max(0, query.offset ?? 0);

  let filtered = all;

  if (query.q) {
    const needle = query.q.trim().toLowerCase();
    filtered = filtered.filter((c) => {
      const hay = `${c.firstName} ${c.lastName} ${c.email}`.toLowerCase();
      return hay.includes(needle);
    });
  }

  if (query.emailDomain) {
    const domain = query.emailDomain.toLowerCase();
    filtered = filtered.filter((c) => c.email.toLowerCase().endsWith(`@${domain}`));
  }

  if (query.hasPhone === true) {
    filtered = filtered.filter((c) => !!c.phone);
  } else if (query.hasPhone === false) {
    filtered = filtered.filter((c) => !c.phone);
  }

  const total = filtered.length;
  const items = filtered.slice(offset, offset + limit);
  return { items, total, limit, offset };
}

function clamp(n: number, min: number, max: number): number {
  if (n < min) return min;
  if (n > max) return max;
  return n;
}

export function sortByName(customers: Customer[]): Customer[] {
  return [...customers].sort((a, b) => {
    const aKey = `${a.lastName} ${a.firstName}`.toLowerCase();
    const bKey = `${b.lastName} ${b.firstName}`.toLowerCase();
    if (aKey < bKey) return -1;
    if (aKey > bKey) return 1;
    return 0;
  });
}
