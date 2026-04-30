"""Tests for service-to-service auth."""
from __future__ import annotations

import pytest
from fastapi import HTTPException

from pii_tokenization.auth import require_service_token


@pytest.mark.asyncio
async def test_missing_token_raises_401():
    with pytest.raises(HTTPException) as exc_info:
        await require_service_token(None)
    assert exc_info.value.status_code == 401


@pytest.mark.asyncio
async def test_invalid_prefix_raises_403():
    with pytest.raises(HTTPException) as exc_info:
        await require_service_token("bad-prefix")
    assert exc_info.value.status_code == 403


@pytest.mark.asyncio
async def test_valid_token_returns_token():
    result = await require_service_token("svc-tok-kyc-enrichment")
    assert result == "svc-tok-kyc-enrichment"


@pytest.mark.asyncio
async def test_valid_token_different_service():
    result = await require_service_token("svc-tok-reporting-aggregator")
    assert result == "svc-tok-reporting-aggregator"
