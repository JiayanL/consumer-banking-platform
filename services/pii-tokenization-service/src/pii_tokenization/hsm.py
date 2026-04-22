"""Stubbed HSM client.

The production service talks to the on-prem HSM cluster managed by the
platform-security team. For this checkout we stub the interface with
an in-process deterministic key-derivation so other services can be
developed against this API without a real HSM.

The real HSM wraps a freshly generated DEK per tokenization; this stub
does NOT — same input produces the same token. Useful for local
testing; obviously not acceptable in production.
"""
from __future__ import annotations

import hashlib
import hmac
from typing import Dict

# Stub key material. The real service pulls these from the HSM; keeping
# them here keeps the dev checkout self-contained.
_STUB_MASTER_KEY = b"dev-stub-master-key-do-not-use-in-prod"
_VAULT: Dict[str, str] = {}
_REVERSE: Dict[str, str] = {}


def tokenize(plaintext: str, *, field: str) -> str:
    """Return a token for ``plaintext``. Idempotent for (plaintext, field)."""
    mac = hmac.new(_STUB_MASTER_KEY, f"{field}:{plaintext}".encode("utf-8"), hashlib.sha256)
    token = f"tok_{field}_{mac.hexdigest()[:24]}"
    _VAULT[token] = plaintext
    _REVERSE[plaintext] = token
    return token


def detokenize(token: str) -> str | None:
    """Return the plaintext previously tokenized under ``token`` or None."""
    return _VAULT.get(token)


def is_known_token(token: str) -> bool:
    return token in _VAULT


def clear() -> None:
    """Test hook. Wipes the in-process vault."""
    _VAULT.clear()
    _REVERSE.clear()
