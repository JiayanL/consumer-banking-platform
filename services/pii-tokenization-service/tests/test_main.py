"""Integration tests for the FastAPI endpoints."""
from __future__ import annotations


def test_healthz(client):
    resp = client.get("/healthz")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


def test_tokenize_endpoint(client):
    resp = client.post("/tokenize", json={"field": "ssn", "value": "123-45-6789"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["field"] == "ssn"
    assert body["token"].startswith("tok_ssn_")


def test_tokenize_bulk_endpoint(client):
    resp = client.post(
        "/tokenize/bulk",
        json={"field": "dob", "values": ["1990-01-01", "1985-06-15"]},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["field"] == "dob"
    assert len(body["tokens"]) == 2


def test_detokenize_endpoint(client):
    tok_resp = client.post("/tokenize", json={"field": "ssn", "value": "999-88-7777"})
    token = tok_resp.json()["token"]

    resp = client.post(
        "/detokenize",
        json={"token": token},
        headers={"X-Service-Token": "svc-tok-kyc-enrichment"},
    )
    assert resp.status_code == 200
    assert resp.json()["value"] == "999-88-7777"


def test_detokenize_missing_auth(client):
    resp = client.post("/detokenize", json={"token": "tok_ssn_x"})
    assert resp.status_code == 401


def test_detokenize_invalid_auth(client):
    resp = client.post(
        "/detokenize",
        json={"token": "tok_ssn_x"},
        headers={"X-Service-Token": "bad-token"},
    )
    assert resp.status_code == 403


def test_detokenize_unknown_token(client):
    resp = client.post(
        "/detokenize",
        json={"token": "tok_ssn_nonexistent"},
        headers={"X-Service-Token": "svc-tok-kyc-enrichment"},
    )
    assert resp.status_code == 404


def test_fingerprint_endpoint(client):
    resp = client.get("/fingerprint/ssn/123-45-6789")
    assert resp.status_code == 200
    assert "fingerprint" in resp.json()


def test_tokenize_invalid_field(client):
    resp = client.post("/tokenize", json={"field": "invalid", "value": "data"})
    assert resp.status_code == 422
