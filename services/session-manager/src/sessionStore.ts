import { randomUUID } from 'crypto';

import type { Session, SessionDraft } from './session';

const DEFAULT_IDLE_MS = 30 * 60 * 1000;

export class SessionStore {
  private readonly sessions = new Map<string, Session>();

  create(draft: SessionDraft): Session {
    const now = Date.now();
    const session: Session = {
      id: randomUUID(),
      userId: draft.userId,
      roles: draft.roles,
      issuedAt: now,
      expiresAt: draft.expiresAt,
      lastSeenAt: now,
      idleTimeoutMs: draft.idleTimeoutMs ?? DEFAULT_IDLE_MS,
    };
    this.sessions.set(session.id, session);
    return session;
  }

  get(id: string): Session | undefined {
    return this.sessions.get(id);
  }

  touch(id: string, at: number = Date.now()): Session | undefined {
    const s = this.sessions.get(id);
    if (!s) return undefined;
    s.lastSeenAt = at;
    return s;
  }

  revoke(id: string): boolean {
    return this.sessions.delete(id);
  }

  listByUser(userId: string): Session[] {
    return Array.from(this.sessions.values()).filter((s) => s.userId === userId);
  }

  size(): number {
    return this.sessions.size;
  }

  reapIdle(now: number = Date.now()): number {
    let reaped = 0;
    for (const [id, s] of this.sessions) {
      if (now - s.lastSeenAt > s.idleTimeoutMs) {
        this.sessions.delete(id);
        reaped++;
      }
    }
    return reaped;
  }
}
