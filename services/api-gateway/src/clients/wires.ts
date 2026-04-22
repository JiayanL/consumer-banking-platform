export interface WireTransfer {
  id: string;
  amountCents: number;
  currency: string;
  fromAccountId: string;
  toAccountId: string;
  status: 'pending' | 'settled' | 'failed';
  initiatedAt: string;
}

/**
 * Stubbed wires client. Returns canned payloads rather than doing any
 * real network IO so the gateway can run entirely offline in CI.
 */
export class WiresClient {
  async initiate(input: Omit<WireTransfer, 'id' | 'status' | 'initiatedAt'>): Promise<WireTransfer> {
    return {
      id: `wire_${Math.floor(Math.random() * 1_000_000)}`,
      status: 'pending',
      initiatedAt: new Date().toISOString(),
      ...input,
    };
  }

  async get(id: string): Promise<WireTransfer> {
    return {
      id,
      amountCents: 50_00,
      currency: 'USD',
      fromAccountId: 'acc_src',
      toAccountId: 'acc_dst',
      status: 'settled',
      initiatedAt: new Date().toISOString(),
    };
  }

  async listForAccount(accountId: string): Promise<WireTransfer[]> {
    return [await this.get(`${accountId}-w1`)];
  }
}
