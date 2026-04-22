"""Format-level validation for PII fields."""
from __future__ import annotations

import datetime as _dt
import re

_SSN_RE = re.compile(r"^\d{3}-\d{2}-\d{4}$")
_ACCT_RE = re.compile(r"^\d{8,17}$")


def is_valid_ssn(value: str) -> bool:
    """True iff ``value`` looks like ``NNN-NN-NNNN`` and is not an
    obvious placeholder (``000-..``, ``666-..``, ``9..``)."""
    if not isinstance(value, str) or not _SSN_RE.match(value):
        return False
    area = value[:3]
    if area == "000" or area == "666" or area.startswith("9"):
        return False
    if value[4:6] == "00":
        return False
    if value[7:] == "0000":
        return False
    return True


def is_valid_account_number(value: str) -> bool:
    """True iff ``value`` is 8–17 digits with no separators."""
    return isinstance(value, str) and bool(_ACCT_RE.match(value))


def is_valid_dob_iso(value: str) -> bool:
    """True iff ``value`` parses as an ISO-8601 date and is in the past
    and within a reasonable human lifespan (≤ 130 years old)."""
    if not isinstance(value, str):
        return False
    try:
        dob = _dt.date.fromisoformat(value)
    except ValueError:
        return False
    today = _dt.date.today()
    if dob >= today:
        return False
    if (today - dob).days > 130 * 366:
        return False
    return True
