import { SessionStore } from '../sessionStore';

describe('SessionStore', () => {
  it('creates and retrieves a session by id', () => {
    const store = new SessionStore();
    const s = store.create({
      userId: 'u1',
      roles: ['customer'],
      expiresAt: Date.now() + 60_000,
      idleTimeoutMs: 5_000,
    });
    expect(s.id).toBeDefined();
    expect(store.get(s.id)?.userId).toBe('u1');
    expect(store.size()).toBe(1);
  });

  it('lists sessions by user', () => {
    const store = new SessionStore();
    store.create({ userId: 'a', roles: [], expiresAt: 1, idleTimeoutMs: 1 });
    store.create({ userId: 'b', roles: [], expiresAt: 1, idleTimeoutMs: 1 });
    store.create({ userId: 'a', roles: [], expiresAt: 1, idleTimeoutMs: 1 });
    expect(store.listByUser('a').length).toBe(2);
  });

  it('revokes a session', () => {
    const store = new SessionStore();
    const s = store.create({ userId: 'u', roles: [], expiresAt: 1, idleTimeoutMs: 1 });
    expect(store.revoke(s.id)).toBe(true);
    expect(store.get(s.id)).toBeUndefined();
  });
});
