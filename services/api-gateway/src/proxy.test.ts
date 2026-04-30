import { findRoute, resolveUpstream, describeRoute, DEFAULT_ROUTES, UpstreamRoute } from './proxy';
import { Request } from 'express';

const ROUTES: UpstreamRoute[] = [
  { prefix: '/api/accounts', upstream: 'http://account-service:8080', stripPrefix: true },
  { prefix: '/api/wires', upstream: 'http://wire-service:8080', stripPrefix: false },
];

function fakeReq(path: string, url?: string): Request {
  return { path, url: url ?? path } as Request;
}

describe('proxy', () => {
  describe('findRoute', () => {
    test('matches exact prefix', () => {
      const match = findRoute(ROUTES, fakeReq('/api/accounts'));
      expect(match).toBeDefined();
      expect(match!.prefix).toBe('/api/accounts');
    });

    test('matches prefix with trailing path', () => {
      const match = findRoute(ROUTES, fakeReq('/api/accounts/acc_1'));
      expect(match).toBeDefined();
      expect(match!.prefix).toBe('/api/accounts');
    });

    test('no match', () => {
      const match = findRoute(ROUTES, fakeReq('/api/unknown'));
      expect(match).toBeUndefined();
    });
  });

  describe('resolveUpstream', () => {
    test('stripPrefix true removes prefix', () => {
      const resolved = resolveUpstream(ROUTES[0], fakeReq('/api/accounts/acc_1'));
      expect(resolved.path).toBe('/acc_1');
      expect(resolved.url).toBe('http://account-service:8080/acc_1');
    });

    test('stripPrefix false keeps full path', () => {
      const resolved = resolveUpstream(ROUTES[1], fakeReq('/api/wires/w1'));
      expect(resolved.path).toBe('/api/wires/w1');
      expect(resolved.url).toBe('http://wire-service:8080/api/wires/w1');
    });

    test('preserves query strings', () => {
      const resolved = resolveUpstream(
        ROUTES[0],
        fakeReq('/api/accounts/acc_1', '/api/accounts/acc_1?status=active'),
      );
      expect(resolved.url).toBe('http://account-service:8080/acc_1?status=active');
    });

    test('stripPrefix on exact prefix yields /', () => {
      const resolved = resolveUpstream(ROUTES[0], fakeReq('/api/accounts'));
      expect(resolved.path).toBe('/');
    });
  });

  describe('describeRoute', () => {
    test('with stripPrefix', () => {
      expect(describeRoute(ROUTES[0])).toBe('/api/accounts -> http://account-service:8080 (strip)');
    });

    test('without stripPrefix', () => {
      expect(describeRoute(ROUTES[1])).toBe('/api/wires -> http://wire-service:8080');
    });
  });

  describe('DEFAULT_ROUTES', () => {
    test('contains expected entries', () => {
      expect(DEFAULT_ROUTES.length).toBeGreaterThanOrEqual(3);
      const prefixes = DEFAULT_ROUTES.map(r => r.prefix);
      expect(prefixes).toContain('/api/accounts');
      expect(prefixes).toContain('/api/wires');
      expect(prefixes).toContain('/api/notify');
    });
  });
});
