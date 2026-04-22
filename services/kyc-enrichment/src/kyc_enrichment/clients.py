"""Stubbed external clients.

Real deployments wire these to httpx-backed clients pointing at the
sanctions / PEP vendors. For now every method returns deterministic
stub data so tests don't need network access.
"""
from __future__ import annotations

from typing import Any


class SanctionsClient:
    """Stub for an OFAC / sanctions list lookup service."""

    def __init__(self, base_url: str = "https://sanctions.example.invalid") -> None:
        self.base_url = base_url

    def check(self, identity_hash: str, name: str) -> dict[str, Any]:
        # Deterministic stub: high-entropy hashes starting with 'f' are
        # flagged so tests can exercise the positive path if they want.
        if identity_hash.startswith("f"):
            return {"matched": True, "list_name": "OFAC-SDN", "score": 0.92}
        return {"matched": False, "list_name": None, "score": 0.0}


class PepClient:
    """Stub for a politically-exposed-person screening service."""

    def __init__(self, base_url: str = "https://pep.example.invalid") -> None:
        self.base_url = base_url

    def check(self, identity_hash: str, name: str) -> dict[str, Any]:
        if identity_hash.startswith("a"):
            return {"matched": True, "role": "Senior Government Official"}
        return {"matched": False, "role": None}
