import { Customer, CommunicationPreferences, CustomerPatch } from './types';

export class CustomerStore {
  private readonly customers = new Map<string, Customer>();
  private readonly prefs = new Map<string, CommunicationPreferences>();
  private seq = 0;

  create(input: Omit<Customer, 'id' | 'createdAt' | 'updatedAt'>): Customer {
    const id = `cust_${++this.seq}`;
    const now = new Date().toISOString();
    const c: Customer = { id, createdAt: now, updatedAt: now, ...input };
    this.customers.set(id, c);
    return c;
  }

  get(id: string): Customer | undefined {
    return this.customers.get(id);
  }

  patch(id: string, patch: CustomerPatch): Customer | undefined {
    const existing = this.customers.get(id);
    if (!existing) return undefined;
    const updated: Customer = {
      ...existing,
      ...patch,
      id: existing.id,
      createdAt: existing.createdAt,
      updatedAt: new Date().toISOString(),
    };
    this.customers.set(id, updated);
    return updated;
  }

  getPreferences(id: string): CommunicationPreferences | undefined {
    return this.prefs.get(id);
  }

  putPreferences(id: string, prefs: Omit<CommunicationPreferences, 'customerId' | 'updatedAt'>): CommunicationPreferences {
    const value: CommunicationPreferences = {
      customerId: id,
      updatedAt: new Date().toISOString(),
      ...prefs,
    };
    this.prefs.set(id, value);
    return value;
  }
}
