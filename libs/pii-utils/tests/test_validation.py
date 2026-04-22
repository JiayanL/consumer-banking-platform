from pii_utils import is_valid_ssn, is_valid_account_number, is_valid_dob_iso


def test_is_valid_ssn_good():
    assert is_valid_ssn("123-45-6789")


def test_is_valid_ssn_bad_format():
    assert not is_valid_ssn("123456789")
    assert not is_valid_ssn("12-345-6789")


def test_is_valid_ssn_rejects_placeholders():
    assert not is_valid_ssn("000-12-3456")
    assert not is_valid_ssn("666-12-3456")
    assert not is_valid_ssn("900-12-3456")
    assert not is_valid_ssn("123-00-4567")
    assert not is_valid_ssn("123-45-0000")


def test_is_valid_account_number():
    assert is_valid_account_number("12345678")
    assert is_valid_account_number("1" * 17)
    assert not is_valid_account_number("1234")
    assert not is_valid_account_number("1234-5678-90")


def test_is_valid_dob_iso():
    assert is_valid_dob_iso("1985-06-15")
    assert not is_valid_dob_iso("not-a-date")
    assert not is_valid_dob_iso("2099-01-01")
    assert not is_valid_dob_iso("1800-01-01")


def test_is_valid_ssn_non_string():
    assert not is_valid_ssn(123456789)  # type: ignore[arg-type]
