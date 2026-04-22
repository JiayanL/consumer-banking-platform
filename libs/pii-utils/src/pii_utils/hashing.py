"""Stable hashing used for de-duplication of PII records across services."""
from __future__ import annotations

import hashlib

from .compliance import ComplianceCategory, compliance_critical


@compliance_critical(category=ComplianceCategory.PII_HANDLING)
def stable_hash(value: str, *, salt: str = "") -> str:
    """Return a hex SHA-256 of ``salt || value``.

    Not intended as a password hash — this is a de-identification helper
    so that two services can agree whether they are looking at the same
    underlying identity without actually exchanging the PII.
    """
    if value is None:
        raise ValueError("value is required")
    h = hashlib.sha256()
    h.update(salt.encode("utf-8"))
    h.update(value.encode("utf-8"))
    return h.hexdigest()
