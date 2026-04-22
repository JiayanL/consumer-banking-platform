export type Session = {
  id: string;
  userId: string;
  roles: string[];
  issuedAt: number;
  expiresAt: number;
  lastSeenAt: number;
  idleTimeoutMs: number;
};

export type SessionDraft = Omit<Session, 'id' | 'issuedAt' | 'lastSeenAt'>;
