import { issueToken, verifyToken, TokenClaims } from './tokens';

export type RotationPolicy = {
  rotateAfterSecs: number;
  hardMaxSecs: number;
};

export const DEFAULT_POLICY: RotationPolicy = {
  rotateAfterSecs: 10 * 60,
  hardMaxSecs: 8 * 60 * 60,
};

export type RotationResult =
  | { rotated: true; token: string; claims: TokenClaims }
  | { rotated: false; reason: 'too-fresh' | 'hard-expired' | 'invalid' };

/**
 * Returns a rotated token if the incoming one is eligible for
 * rotation under the provided policy. Preserves the original `sid`.
 *
 * @compliance-critical AUTHENTICATION
 */
export function maybeRotate(
  secret: string,
  token: string,
  policy: RotationPolicy = DEFAULT_POLICY,
): RotationResult {
  const verified = verifyToken(secret, token);
  if (!verified.valid) return { rotated: false, reason: 'invalid' };
  const claims = verified.claims;
  const now = Math.floor(Date.now() / 1000);
  const age = now - claims.iat;
  if (age >= policy.hardMaxSecs) return { rotated: false, reason: 'hard-expired' };
  if (age < policy.rotateAfterSecs) return { rotated: false, reason: 'too-fresh' };

  const rotated = issueToken(secret, { sub: claims.sub, roles: claims.roles, sid: claims.sid });
  return { rotated: true, token: rotated, claims };
}

export function shouldRotate(
  iat: number,
  now: number = Math.floor(Date.now() / 1000),
  policy: RotationPolicy = DEFAULT_POLICY,
): boolean {
  const age = now - iat;
  return age >= policy.rotateAfterSecs && age < policy.hardMaxSecs;
}
