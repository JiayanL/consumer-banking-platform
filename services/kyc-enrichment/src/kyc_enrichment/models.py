"""Pydantic request / response models for KYC enrichment."""
from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, Field


RiskTier = Literal["LOW", "MEDIUM", "HIGH"]


class EnrichRequest(BaseModel):
    ssn: str = Field(..., description="SSN in NNN-NN-NNNN form")
    dob: str = Field(..., description="ISO-8601 date of birth, YYYY-MM-DD")
    name: str = Field(..., min_length=1)


class SanctionsMatch(BaseModel):
    matched: bool
    list_name: Optional[str] = None
    score: float = 0.0


class PepMatch(BaseModel):
    matched: bool
    role: Optional[str] = None


class EnrichedRecord(BaseModel):
    customer_id: str
    name: str
    masked_ssn: str
    identity_hash: str
    risk_tier: RiskTier
    sanctions: SanctionsMatch
    pep: PepMatch
    status: Literal["PENDING", "VERIFIED", "REJECTED"] = "VERIFIED"


class StatusResponse(BaseModel):
    customer_id: str
    status: Literal["PENDING", "VERIFIED", "REJECTED"]
    risk_tier: RiskTier
