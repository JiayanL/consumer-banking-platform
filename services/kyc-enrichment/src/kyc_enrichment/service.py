"""Core KYC enrichment business logic."""
from __future__ import annotations

from typing import Optional
from uuid import uuid4

from pii_utils import (
    ComplianceCategory,
    compliance_critical,
    is_valid_dob_iso,
    is_valid_ssn,
    mask_ssn,
    stable_hash,
)

from .clients import PepClient, SanctionsClient
from .models import (
    EnrichedRecord,
    EnrichRequest,
    PepMatch,
    RiskTier,
    SanctionsMatch,
    StatusResponse,
)


_IDENTITY_SALT = "kyc-enrichment/v1"


class EnrichmentError(ValueError):
    """Raised when a payload fails format validation."""


class KycService:
    def __init__(
        self,
        sanctions: Optional[SanctionsClient] = None,
        pep: Optional[PepClient] = None,
    ) -> None:
        self.sanctions = sanctions or SanctionsClient()
        self.pep = pep or PepClient()
        self._records: dict[str, EnrichedRecord] = {}

    @compliance_critical(category=ComplianceCategory.PII_HANDLING)
    def enrich(self, payload: EnrichRequest) -> EnrichedRecord:
        """Enrich a raw KYC payload. Handles raw SSN, so it is tagged
        as compliance-critical under PII_HANDLING."""
        if not is_valid_ssn(payload.ssn):
            raise EnrichmentError("invalid ssn format")
        if not is_valid_dob_iso(payload.dob):
            raise EnrichmentError("invalid dob; expected YYYY-MM-DD in the past")

        identity_hash = self._hash_identity(payload.ssn, payload.dob)
        sanctions_raw = self.sanctions.check(identity_hash, payload.name)
        pep_raw = self.pep.check(identity_hash, payload.name)

        sanctions = SanctionsMatch(**sanctions_raw)
        pep = PepMatch(**pep_raw)
        risk_tier = self._compute_risk(sanctions, pep)

        record = EnrichedRecord(
            customer_id=f"cust_{uuid4().hex[:12]}",
            name=payload.name,
            masked_ssn=mask_ssn(payload.ssn),
            identity_hash=identity_hash,
            risk_tier=risk_tier,
            sanctions=sanctions,
            pep=pep,
            status="VERIFIED",
        )
        self._records[record.customer_id] = record
        return record

    @compliance_critical(category=ComplianceCategory.PII_HANDLING)
    def _hash_identity(self, ssn: str, dob: str) -> str:
        """Wrap stable_hash so the caller is captured as PII-handling."""
        return stable_hash(f"{ssn}|{dob}", salt=_IDENTITY_SALT)

    @staticmethod
    def _compute_risk(sanctions: SanctionsMatch, pep: PepMatch) -> RiskTier:
        if sanctions.matched:
            return "HIGH"
        if pep.matched:
            return "MEDIUM"
        return "LOW"

    def status(self, customer_id: str) -> StatusResponse:
        rec = self._records.get(customer_id)
        if rec is None:
            raise KeyError(customer_id)
        return StatusResponse(
            customer_id=rec.customer_id,
            status=rec.status,
            risk_tier=rec.risk_tier,
        )

    def reverify(self, customer_id: str) -> EnrichedRecord:
        rec = self._records.get(customer_id)
        if rec is None:
            raise KeyError(customer_id)
        # Re-run sanctions/PEP lookups against the cached identity hash.
        sanctions = SanctionsMatch(**self.sanctions.check(rec.identity_hash, rec.name))
        pep = PepMatch(**self.pep.check(rec.identity_hash, rec.name))
        rec = rec.model_copy(
            update={
                "sanctions": sanctions,
                "pep": pep,
                "risk_tier": self._compute_risk(sanctions, pep),
            }
        )
        self._records[customer_id] = rec
        return rec
