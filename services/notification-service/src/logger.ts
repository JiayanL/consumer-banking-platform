import { createLogger, Logger } from '@cbp/logging-sdk';

export const logger: Logger = createLogger({
  service: 'notification-service',
  level: (process.env.LOG_LEVEL as 'debug' | 'info' | 'warn' | 'error' | undefined) ?? 'info',
});
