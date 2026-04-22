import { createApp } from './app';
import { logger } from './logger';

const port = Number(process.env.PORT ?? 8080);
const app = createApp();

app.listen(port, () => {
  logger.info('notification-service.listening', { port });
});
