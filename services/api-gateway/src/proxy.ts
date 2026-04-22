import { Request } from 'express';

export interface UpstreamRoute {
  prefix: string;
  upstream: string;
  stripPrefix?: boolean;
}

export interface ResolvedUpstream {
  route: UpstreamRoute;
  path: string;
  url: string;
}

/**
 * Deterministic prefix-based routing table. This intentionally does
 * NOT make real network calls — the gateway's downstream clients are
 * stubbed. The helpers below exist so we can lift the routing table
 * into config later without touching call sites.
 */
export function findRoute(routes: UpstreamRoute[], req: Request): UpstreamRoute | undefined {
  return routes.find((r) => req.path === r.prefix || req.path.startsWith(r.prefix + '/'));
}

export function resolveUpstream(route: UpstreamRoute, req: Request): ResolvedUpstream {
  let path = req.path;
  if (route.stripPrefix) {
    path = path.slice(route.prefix.length) || '/';
  }
  const qs = req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '';
  return { route, path, url: `${route.upstream}${path}${qs}` };
}

export function describeRoute(route: UpstreamRoute): string {
  return `${route.prefix} -> ${route.upstream}${route.stripPrefix ? ' (strip)' : ''}`;
}

export const DEFAULT_ROUTES: UpstreamRoute[] = [
  { prefix: '/api/accounts', upstream: 'http://account-service:8080', stripPrefix: true },
  { prefix: '/api/wires', upstream: 'http://wire-service:8080', stripPrefix: true },
  { prefix: '/api/notify', upstream: 'http://notification-service:8080', stripPrefix: true },
];
