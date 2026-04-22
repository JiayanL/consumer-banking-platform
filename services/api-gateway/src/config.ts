export interface GatewayConfig {
  port: number;
  logLevel: 'debug' | 'info' | 'warn' | 'error';
  rateLimit: {
    windowMs: number;
    max: number;
  };
  upstream: {
    accounts: string;
    wires: string;
    notify: string;
  };
  requireAuth: boolean;
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env): GatewayConfig {
  return {
    port: parseIntOr(env.PORT, 8080),
    logLevel: (env.LOG_LEVEL as GatewayConfig['logLevel']) ?? 'info',
    rateLimit: {
      windowMs: parseIntOr(env.RATE_LIMIT_WINDOW_MS, 60_000),
      max: parseIntOr(env.RATE_LIMIT_MAX, 120),
    },
    upstream: {
      accounts: env.UPSTREAM_ACCOUNTS ?? 'http://account-service:8080',
      wires: env.UPSTREAM_WIRES ?? 'http://wire-service:8080',
      notify: env.UPSTREAM_NOTIFY ?? 'http://notification-service:8080',
    },
    requireAuth: env.REQUIRE_AUTH !== 'false',
  };
}

function parseIntOr(v: string | undefined, fallback: number): number {
  if (v === undefined) return fallback;
  const n = Number.parseInt(v, 10);
  return Number.isFinite(n) ? n : fallback;
}
