import { loadConfig } from './config';

describe('loadConfig', () => {
  test('default env returns all defaults', () => {
    const cfg = loadConfig({} as NodeJS.ProcessEnv);
    expect(cfg.port).toBe(8080);
    expect(cfg.logLevel).toBe('info');
    expect(cfg.rateLimit.windowMs).toBe(60_000);
    expect(cfg.rateLimit.max).toBe(120);
    expect(cfg.requireAuth).toBe(true);
  });

  test('custom env vars override defaults', () => {
    const env = {
      PORT: '3000',
      LOG_LEVEL: 'debug',
      RATE_LIMIT_WINDOW_MS: '30000',
      RATE_LIMIT_MAX: '50',
      UPSTREAM_ACCOUNTS: 'http://custom:9000',
      REQUIRE_AUTH: 'false',
    } as unknown as NodeJS.ProcessEnv;

    const cfg = loadConfig(env);
    expect(cfg.port).toBe(3000);
    expect(cfg.logLevel).toBe('debug');
    expect(cfg.rateLimit.windowMs).toBe(30_000);
    expect(cfg.rateLimit.max).toBe(50);
    expect(cfg.upstream.accounts).toBe('http://custom:9000');
    expect(cfg.requireAuth).toBe(false);
  });

  test('parseIntOr handles invalid string', () => {
    const env = { PORT: 'not-a-number' } as unknown as NodeJS.ProcessEnv;
    const cfg = loadConfig(env);
    expect(cfg.port).toBe(8080);
  });

  test('requireAuth defaults to true when env is anything other than false', () => {
    const env1 = { REQUIRE_AUTH: 'true' } as unknown as NodeJS.ProcessEnv;
    expect(loadConfig(env1).requireAuth).toBe(true);

    const env2 = { REQUIRE_AUTH: 'yes' } as unknown as NodeJS.ProcessEnv;
    expect(loadConfig(env2).requireAuth).toBe(true);
  });
});
