"""Tests for the tokenization service logic."""
from __future__ import annotations

from pii_tokenization import service


def test_tokenize_happy_path():
    token = service.tokenize("ssn", "123-45-6789")
    assert token.startswith("tok_ssn_")
    assert len(token) > len("tok_ssn_")


def test_detokenize_round_trip():
    token = service.tokenize("ssn", "123-45-6789")
    plain = service.detokenize(token)
    assert plain == "123-45-6789"


def test_tokenize_is_idempotent():
    t1 = service.tokenize("ssn", "111-22-3333")
    t2 = service.tokenize("ssn", "111-22-3333")
    assert t1 == t2


def test_tokenize_different_fields_produce_different_tokens():
    t1 = service.tokenize("ssn", "value")
    t2 = service.tokenize("dob", "value")
    assert t1 != t2


def test_detokenize_unknown_token_returns_none():
    result = service.detokenize("tok_ssn_nonexistent")
    assert result is None


def test_safe_display_ssn():
    masked = service.safe_display("ssn", "123-45-6789")
    assert masked == "***-**-6789"
    assert "123" not in masked


def test_safe_display_account_number():
    masked = service.safe_display("account_number", "1234567890")
    assert masked != "1234567890"


def test_safe_display_unknown_field():
    assert service.safe_display("other", "secret") == "***"


def test_fingerprint_deterministic():
    f1 = service.fingerprint("ssn", "123-45-6789")
    f2 = service.fingerprint("ssn", "123-45-6789")
    assert f1 == f2


def test_fingerprint_different_values():
    f1 = service.fingerprint("ssn", "111-11-1111")
    f2 = service.fingerprint("ssn", "222-22-2222")
    assert f1 != f2
