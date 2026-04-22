import { NotificationRecord } from './store';
import { logger } from './logger';

export interface DeliveryResult {
  id: string;
  deliveredAt: string;
  attempts: number;
}

/**
 * Stubbed delivery backends. In a real environment these would call
 * SES / SNS / Twilio. Here we simulate a delivery with a sleep + a
 * couple of retry tiers so the operational story is recognisable.
 */
export class EmailDeliverer {
  async deliver(record: NotificationRecord): Promise<DeliveryResult> {
    const attempts = await this.attemptWithBackoff(async () => {
      await sleep(5);
      logger.debug('email.attempt', { id: record.id });
    });
    return {
      id: record.id,
      deliveredAt: new Date().toISOString(),
      attempts,
    };
  }

  private async attemptWithBackoff(fn: () => Promise<void>): Promise<number> {
    let lastErr: unknown = null;
    for (let i = 1; i <= 3; i++) {
      try {
        await fn();
        return i;
      } catch (err) {
        lastErr = err;
        await sleep(i * 10);
      }
    }
    throw lastErr ?? new Error('delivery failed');
  }
}

export class SmsDeliverer {
  async deliver(record: NotificationRecord): Promise<DeliveryResult> {
    await sleep(5);
    logger.debug('sms.attempt', { id: record.id });
    return {
      id: record.id,
      deliveredAt: new Date().toISOString(),
      attempts: 1,
    };
  }
}

export function chooseDeliverer(channel: string): EmailDeliverer | SmsDeliverer {
  if (channel === 'email') return new EmailDeliverer();
  if (channel === 'sms') return new SmsDeliverer();
  throw new Error(`unknown channel: ${channel}`);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
