"""Tests for kyc-enrichment."""
from fastapi.testclient import TestClient

from kyc_enrichment.main import create_app


def test_enrich_happy_path():
    client = TestClient(create_app())
    resp = client.post(
        "/kyc/enrich",
        json={"ssn": "123-45-6789", "dob": "1990-01-15", "name": "Jane Doe"},
    )
    assert resp.status_code == 201
    body = resp.json()
    assert body["name"] == "Jane Doe"
    assert body["masked_ssn"] == "***-**-6789"
    assert body["risk_tier"] in {"LOW", "MEDIUM", "HIGH"}
    assert body["customer_id"].startswith("cust_")


def test_enrich_invalid_ssn_returns_422():
    client = TestClient(create_app())
    resp = client.post(
        "/kyc/enrich",
        json={"ssn": "000-00-0000", "dob": "1990-01-15", "name": "Jane Doe"},
    )
    assert resp.status_code == 422
