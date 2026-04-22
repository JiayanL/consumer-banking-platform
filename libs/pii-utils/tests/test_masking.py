import pytest

from pii_utils import mask_ssn, mask_account_number, mask_email


class TestMaskSSN:
    def test_dashed_input(self):
        assert mask_ssn("123-45-6789") == "***-**-6789"

    def test_digits_only(self):
        assert mask_ssn("123456789") == "***-**-6789"

    def test_rejects_none(self):
        with pytest.raises(ValueError):
            mask_ssn(None)  # type: ignore[arg-type]

    def test_rejects_wrong_length(self):
        with pytest.raises(ValueError):
            mask_ssn("12345")


class TestMaskAccountNumber:
    def test_long_number(self):
        assert mask_account_number("1234567890") == "******7890"

    def test_rejects_short(self):
        with pytest.raises(ValueError):
            mask_account_number("12")


class TestMaskEmail:
    def test_standard(self):
        assert mask_email("priya@example.com") == "p***a@example.com"

    def test_short_local(self):
        assert mask_email("ab@x.com") == "**@x.com"

    def test_not_an_email(self):
        assert mask_email("not-an-email") == "***"
