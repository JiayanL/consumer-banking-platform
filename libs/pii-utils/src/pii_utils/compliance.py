"""Compliance-critical annotation for Python services.

The decorator is a no-op at runtime — it just stores the category on
the wrapped callable so the coverage dashboard can find it later via
AST parsing or reflection.
"""
from __future__ import annotations

from enum import Enum
from functools import wraps
from typing import Any, Callable, TypeVar


class ComplianceCategory(str, Enum):
    """OCC-mapped categories. Kept in sync with the Java + TS sides."""

    TRANSACTION_INTEGRITY = "TRANSACTION_INTEGRITY"
    AUTHENTICATION = "AUTHENTICATION"
    PII_HANDLING = "PII_HANDLING"
    AUDIT_TRAIL = "AUDIT_TRAIL"


F = TypeVar("F", bound=Callable[..., Any])


def compliance_critical(
    *,
    category: str | ComplianceCategory,
    note: str | None = None,
) -> Callable[[F], F]:
    """Mark a callable as compliance-critical.

    Runtime-wise this is a passthrough. The coverage dashboard parses
    these decorators out of source.
    """
    normalized = ComplianceCategory(category) if not isinstance(category, ComplianceCategory) else category

    def decorator(fn: F) -> F:
        @wraps(fn)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            return fn(*args, **kwargs)

        wrapper.__compliance_critical__ = {  # type: ignore[attr-defined]
            "category": normalized.value,
            "note": note,
        }
        return wrapper  # type: ignore[return-value]

    return decorator
