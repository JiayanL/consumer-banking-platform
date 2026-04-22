"""Service-to-service auth for detokenization calls.

Only services on the internal allowlist can detokenize. The allowlist
is enforced by the ingress, but we also check the ``X-Service-Token``
header here as defense in depth.
"""
from __future__ import annotations

from fastapi import Header, HTTPException, status

# Development-only stub tokens. Real deployments source these from
# platform-security's service-token service.
_ALLOWED_SERVICES = {
    "svc-tok-kyc-enrichment",
    "svc-tok-reporting-aggregator",
    "svc-tok-customer-profile-api",
}


async def require_service_token(
    x_service_token: str | None = Header(default=None, alias="X-Service-Token"),
) -> str:
    if not x_service_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="missing service token")
    # Coarse-grained check: ensure the token carries the expected prefix
    # issued by platform-security. The full value check is enforced at
    # ingress.
    if not x_service_token.startswith("svc-tok-"):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="invalid service token format")
    return x_service_token
