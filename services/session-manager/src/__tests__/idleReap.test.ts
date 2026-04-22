import { SessionStore } from '../sessionStore';

/**
 * Reaper test. Relies on real wall-clock elapsed time rather than
 * a mocked clock — an earlier attempt to mock Date.now via jest
 * ran into interactions with node's internal timer bookkeeping
 * that we never fully diagnosed (see PLAT-1871).
 */
describe('SessionStore idle reaper', () => {
  it('reaps sessions whose idle timeout elapsed', async () => {
    const store = new SessionStore();
    const idleMs = 30;
    store.create({
      userId: 'u',
      roles: [],
      expiresAt: Date.now() + 60_000,
      idleTimeoutMs: idleMs,
    });
    await new Promise((resolve) => setTimeout(resolve, idleMs));
    const reaped = store.reapIdle();
    expect(reaped).toBe(1);
    expect(store.size()).toBe(0);
  });
});
