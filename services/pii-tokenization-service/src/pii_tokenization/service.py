"""Tokenization service logic.

Uses the pii-utils shared library for masking/hashing but NOT for
validation on the way in — the validation step is handled at the
boundary (the FastAPI route) which currently does the minimal work
needed to get a token back.
"""
from __future__ import annotations

from pii_utils import (
    ComplianceCategory,
    compliance_critical,
    mask_account_number,
    mask_ssn,
    stable_hash,
)

from . import hsm


@compliance_critical(category=ComplianceCategory.PII_HANDLING)
def tokenize(field: str, value: str) -> str:
    return hsm.tokenize(value, field=field)


@compliance_critical(category=ComplianceCategory.PII_HANDLING)
def detokenize(token: str) -> str | None:
    return hsm.detokenize(token)


def safe_display(field: str, value: str) -> str:
    """Return a masked representation suitable for logs / UIs."""
    if field == "ssn":
        try:
            return mask_ssn(value)
        except ValueError:
            return "***"
    if field == "account_number":
        try:
            return mask_account_number(value)
        except ValueError:
            return "***"
    return "***"


def fingerprint(field: str, value: str) -> str:
    return stable_hash(value, salt=f"pii:{field}")
