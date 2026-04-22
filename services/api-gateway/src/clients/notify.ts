export interface NotifyRequest {
  to: string;
  subject?: string;
  body: string;
  channel: 'email' | 'sms';
}

export interface NotifyReceipt {
  id: string;
  acceptedAt: string;
}

/**
 * Stubbed notification client. Mirrors the notification-service REST
 * contract so the gateway can forward shape without validating it
 * itself.
 */
export class NotifyClient {
  async send(input: NotifyRequest): Promise<NotifyReceipt> {
    return {
      id: `ntf_${Math.floor(Math.random() * 1_000_000)}`,
      acceptedAt: new Date().toISOString(),
    };
  }

  async status(id: string): Promise<{ id: string; status: 'queued' | 'delivered' | 'failed' }> {
    return { id, status: 'delivered' };
  }
}
