"""Tests for the stubbed HSM client."""
from __future__ import annotations

from pii_tokenization import hsm


def test_tokenize_round_trip():
    token = hsm.tokenize("secret-value", field="ssn")
    assert hsm.detokenize(token) == "secret-value"


def test_tokenize_idempotent():
    t1 = hsm.tokenize("val", field="dob")
    t2 = hsm.tokenize("val", field="dob")
    assert t1 == t2


def test_detokenize_unknown_returns_none():
    assert hsm.detokenize("tok_ssn_does_not_exist") is None


def test_is_known_token():
    token = hsm.tokenize("data", field="account_number")
    assert hsm.is_known_token(token) is True
    assert hsm.is_known_token("tok_bogus") is False


def test_empty_string_tokenize():
    token = hsm.tokenize("", field="ssn")
    assert hsm.detokenize(token) == ""


def test_long_input_tokenize():
    long_value = "A" * 10_000
    token = hsm.tokenize(long_value, field="ssn")
    assert hsm.detokenize(token) == long_value


def test_clear_wipes_vault():
    hsm.tokenize("val", field="ssn")
    hsm.clear()
    assert hsm.detokenize("tok_ssn_anything") is None
