"""Masking helpers for PII fields."""
from __future__ import annotations

from .compliance import ComplianceCategory, compliance_critical


@compliance_critical(category=ComplianceCategory.PII_HANDLING)
def mask_ssn(ssn: str) -> str:
    """Return a masked SSN of the form ``***-**-1234``.

    Accepts either ``123-45-6789`` or ``123456789`` input. Raises
    ``ValueError`` if the input is obviously not an SSN.
    """
    if ssn is None:
        raise ValueError("ssn is required")
    digits = "".join(c for c in ssn if c.isdigit())
    if len(digits) != 9:
        raise ValueError("ssn must contain exactly 9 digits")
    return f"***-**-{digits[-4:]}"


@compliance_critical(category=ComplianceCategory.PII_HANDLING)
def mask_account_number(acct: str) -> str:
    """Masks all but the last 4 digits of an account number."""
    if acct is None or len(acct) < 4:
        raise ValueError("account number too short to mask safely")
    return "*" * (len(acct) - 4) + acct[-4:]


def mask_email(email: str) -> str:
    """Partially masks an email for display. Not a reversible operation."""
    if email is None or "@" not in email:
        return "***"
    local, _, domain = email.partition("@")
    if len(local) <= 2:
        masked_local = "*" * len(local)
    else:
        masked_local = local[0] + "*" * (len(local) - 2) + local[-1]
    return f"{masked_local}@{domain}"
