"""Shared fixtures for pii-tokenization-service tests."""
from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from pii_tokenization import hsm
from pii_tokenization.main import app


@pytest.fixture(autouse=True)
def _clear_hsm():
    """Reset the in-process token vault between tests."""
    hsm.clear()
    yield
    hsm.clear()


@pytest.fixture()
def client():
    return TestClient(app)
