"""Admin endpoints and helpers (untested — internal-only, gated by SSO).

Exposes operator-facing endpoints for listing pending verifications and
manually overriding a customer's risk tier. Wiring into the main app is
intentionally disabled pending a security review.
"""
from __future__ import annotations

from typing import Literal

from fastapi import APIRouter, HTTPException

from .models import RiskTier, StatusResponse
from .service import KycService


def build_admin_router(svc: KycService) -> APIRouter:
    router = APIRouter(prefix="/admin/kyc", tags=["admin"])

    @router.get("/records")
    def list_records() -> list[StatusResponse]:
        return [
            StatusResponse(
                customer_id=r.customer_id,
                status=r.status,
                risk_tier=r.risk_tier,
            )
            for r in svc._records.values()
        ]

    @router.post("/records/{customer_id}/override")
    def override_risk(
        customer_id: str,
        new_tier: RiskTier,
    ) -> StatusResponse:
        rec = svc._records.get(customer_id)
        if rec is None:
            raise HTTPException(status_code=404, detail="customer not found")
        rec = rec.model_copy(update={"risk_tier": new_tier})
        svc._records[customer_id] = rec
        return StatusResponse(
            customer_id=rec.customer_id,
            status=rec.status,
            risk_tier=rec.risk_tier,
        )

    @router.post("/records/{customer_id}/status")
    def set_status(
        customer_id: str,
        new_status: Literal["PENDING", "VERIFIED", "REJECTED"],
    ) -> StatusResponse:
        rec = svc._records.get(customer_id)
        if rec is None:
            raise HTTPException(status_code=404, detail="customer not found")
        rec = rec.model_copy(update={"status": new_status})
        svc._records[customer_id] = rec
        return StatusResponse(
            customer_id=rec.customer_id,
            status=rec.status,
            risk_tier=rec.risk_tier,
        )

    return router
