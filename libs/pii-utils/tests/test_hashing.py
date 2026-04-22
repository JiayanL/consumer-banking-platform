import pytest

from pii_utils import stable_hash


def test_deterministic():
    assert stable_hash("abc") == stable_hash("abc")


def test_salt_changes_output():
    assert stable_hash("abc", salt="s1") != stable_hash("abc", salt="s2")


def test_rejects_none():
    with pytest.raises(ValueError):
        stable_hash(None)  # type: ignore[arg-type]
