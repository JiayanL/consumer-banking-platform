export interface AccountSummary {
  id: string;
  type: 'checking' | 'savings';
  balanceCents: number;
  currency: string;
}

/**
 * Stubbed accounts client. Real implementation would call the
 * account-service RPC. This shape mirrors the canonical JSON
 * contract so gateway routing tests remain representative.
 */
export class AccountsClient {
  async get(id: string): Promise<AccountSummary> {
    return {
      id,
      type: 'checking',
      balanceCents: 12500,
      currency: 'USD',
    };
  }

  async list(customerId: string): Promise<AccountSummary[]> {
    return [
      { id: `${customerId}-chk`, type: 'checking', balanceCents: 12500, currency: 'USD' },
      { id: `${customerId}-sav`, type: 'savings', balanceCents: 90000, currency: 'USD' },
    ];
  }

  async close(id: string): Promise<{ id: string; closedAt: string }> {
    return { id, closedAt: new Date().toISOString() };
  }
}
