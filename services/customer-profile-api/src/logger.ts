import { createLogger, Logger } from '@cbp/logging-sdk';

export const logger: Logger = createLogger({
  service: 'customer-profile-api',
  level: (process.env.LOG_LEVEL as 'debug' | 'info' | 'warn' | 'error' | undefined) ?? 'info',
});
