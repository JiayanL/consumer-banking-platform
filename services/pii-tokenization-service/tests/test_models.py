"""Tests for Pydantic request/response models."""
from __future__ import annotations

import pytest
from pydantic import ValidationError

from pii_tokenization.models import (
    BulkTokenizeRequest,
    BulkTokenizeResponse,
    DetokenizeRequest,
    DetokenizeResponse,
    TokenizeRequest,
    TokenizeResponse,
)


def test_tokenize_request_valid():
    req = TokenizeRequest(field="ssn", value="123-45-6789")
    assert req.field == "ssn"
    assert req.value == "123-45-6789"


def test_tokenize_request_invalid_field():
    with pytest.raises(ValidationError):
        TokenizeRequest(field="invalid_field", value="123")


def test_tokenize_request_missing_value():
    with pytest.raises(ValidationError):
        TokenizeRequest(field="ssn")


def test_tokenize_response_valid():
    resp = TokenizeResponse(token="tok_ssn_abc", field="ssn")
    assert resp.token == "tok_ssn_abc"


def test_detokenize_request_valid():
    req = DetokenizeRequest(token="tok_ssn_abc123")
    assert req.token == "tok_ssn_abc123"


def test_detokenize_response_valid():
    resp = DetokenizeResponse(value="123-45-6789")
    assert resp.value == "123-45-6789"


def test_bulk_tokenize_request_valid():
    req = BulkTokenizeRequest(field="dob", values=["1990-01-01", "1985-06-15"])
    assert req.field == "dob"
    assert len(req.values) == 2


def test_bulk_tokenize_response_valid():
    resp = BulkTokenizeResponse(tokens=["tok1", "tok2"], field="ssn")
    assert resp.errors is None
    assert len(resp.tokens) == 2


def test_bulk_tokenize_response_with_errors():
    resp = BulkTokenizeResponse(tokens=["tok1"], field="ssn", errors=["bad input"])
    assert resp.errors == ["bad input"]
