import jwt from 'jsonwebtoken';

export type TokenClaims = {
  sub: string;
  roles: string[];
  sid: string;
  iat: number;
  exp: number;
};

export type VerifyResult =
  | { valid: true; claims: TokenClaims }
  | { valid: false; reason: 'missing' | 'malformed' | 'expired' | 'bad-signature' };

const DEFAULT_TTL_SECS = 15 * 60;

/** @compliance-critical AUTHENTICATION */
export function issueToken(
  secret: string,
  payload: Omit<TokenClaims, 'iat' | 'exp'>,
  ttlSecs: number = DEFAULT_TTL_SECS,
): string {
  const now = Math.floor(Date.now() / 1000);
  const claims: TokenClaims = { ...payload, iat: now, exp: now + ttlSecs };
  return jwt.sign(claims, secret, { algorithm: 'HS256' });
}

/** @compliance-critical AUTHENTICATION */
export function verifyToken(secret: string, token: string | undefined): VerifyResult {
  if (!token) return { valid: false, reason: 'missing' };
  const stripped = token.startsWith('Bearer ') ? token.slice('Bearer '.length) : token;

  let decoded: jwt.JwtPayload | string;
  try {
    decoded = jwt.verify(stripped, secret, { algorithms: ['HS256'] });
  } catch (err: unknown) {
    if (err instanceof jwt.TokenExpiredError) return { valid: false, reason: 'expired' };
    if (err instanceof jwt.JsonWebTokenError) return { valid: false, reason: 'bad-signature' };
    return { valid: false, reason: 'malformed' };
  }

  if (typeof decoded !== 'object' || !decoded) {
    return { valid: false, reason: 'malformed' };
  }
  const claims = decoded as Partial<TokenClaims>;
  if (!claims.sub || !claims.sid || typeof claims.exp !== 'number' || typeof claims.iat !== 'number') {
    return { valid: false, reason: 'malformed' };
  }

  const now = Math.floor(Date.now() / 1000);
  if (claims.exp > now) {
    return {
      valid: true,
      claims: {
        sub: claims.sub,
        roles: claims.roles ?? [],
        sid: claims.sid,
        iat: claims.iat,
        exp: claims.exp,
      },
    };
  }
  return { valid: false, reason: 'expired' };
}
