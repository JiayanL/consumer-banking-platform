"""Tests for enrichment_pipeline.py — address, phone, device enrichment stages.

Covers AddressEnricher, PhoneEnricher, DeviceEnricher, and ExtendedPipeline.
OCC category: PII_HANDLING. Jira: KAN-6.
"""
from kyc_enrichment.enrichment_pipeline import (
    AddressEnricher,
    AddressEnrichment,
    DeviceEnricher,
    DeviceEnrichment,
    ExtendedPipeline,
    PhoneEnricher,
    PhoneEnrichment,
)
from kyc_enrichment.models import EnrichedRecord, PepMatch, SanctionsMatch


def _make_record(**overrides) -> EnrichedRecord:
    """Factory for a minimal EnrichedRecord used as pipeline input."""
    defaults = {
        "customer_id": "cust_test123",
        "name": "Jane Doe",
        "masked_ssn": "***-**-6789",
        "identity_hash": "abc123hash",
        "risk_tier": "LOW",
        "sanctions": SanctionsMatch(matched=False),
        "pep": PepMatch(matched=False),
    }
    defaults.update(overrides)
    return EnrichedRecord(**defaults)


# ---------------------------------------------------------------------------
# AddressEnricher
# ---------------------------------------------------------------------------


class TestAddressEnricher:
    def test_full_address_parsed_and_uppercased(self):
        enricher = AddressEnricher()
        result = enricher.enrich("123 Main St, Springfield, IL 62704")

        assert isinstance(result, AddressEnrichment)
        assert result.normalized_line1 == "123 MAIN ST"
        assert result.normalized_city == "SPRINGFIELD"
        assert result.normalized_state == "IL"
        assert result.normalized_postal == "62704"
        assert result.usps_deliverable is True

    def test_address_with_only_line1(self):
        enricher = AddressEnricher()
        result = enricher.enrich("PO Box 100")

        assert result.normalized_line1 == "PO BOX 100"
        assert result.normalized_city == ""
        assert result.normalized_state == ""
        assert result.normalized_postal == ""

    def test_address_with_line1_and_city_only(self):
        enricher = AddressEnricher()
        result = enricher.enrich("500 Broadway, New York")

        assert result.normalized_line1 == "500 BROADWAY"
        assert result.normalized_city == "NEW YORK"
        assert result.normalized_state == ""
        assert result.normalized_postal == ""

    def test_empty_address(self):
        enricher = AddressEnricher()
        result = enricher.enrich("")

        assert result.normalized_line1 == ""
        assert result.normalized_city == ""
        assert result.normalized_state == ""
        assert result.normalized_postal == ""

    def test_custom_base_url(self):
        enricher = AddressEnricher(base_url="https://custom.addr.test")
        assert enricher.base_url == "https://custom.addr.test"

    def test_default_base_url(self):
        enricher = AddressEnricher()
        assert enricher.base_url == "https://addr.example.invalid"


# ---------------------------------------------------------------------------
# PhoneEnricher
# ---------------------------------------------------------------------------


class TestPhoneEnricher:
    def test_ten_digit_us_number(self):
        enricher = PhoneEnricher()
        result = enricher.enrich("(555) 867-5309")

        assert isinstance(result, PhoneEnrichment)
        assert result.e164 == "+15558675309"
        assert result.carrier is None
        assert result.line_type == "unknown"

    def test_eleven_digit_us_number_with_country_code(self):
        enricher = PhoneEnricher()
        result = enricher.enrich("1-555-867-5309")

        assert result.e164 == "+15558675309"

    def test_non_standard_length_returned_as_is(self):
        enricher = PhoneEnricher()
        result = enricher.enrich("+44 20 7946 0958")

        assert result.e164 == "+44 20 7946 0958"

    def test_short_number_returned_as_is(self):
        enricher = PhoneEnricher()
        result = enricher.enrich("911")

        assert result.e164 == "911"

    def test_digits_only_input(self):
        enricher = PhoneEnricher()
        result = enricher.enrich("5558675309")

        assert result.e164 == "+15558675309"

    def test_custom_base_url(self):
        enricher = PhoneEnricher(base_url="https://phone.test")
        assert enricher.base_url == "https://phone.test"

    def test_default_base_url(self):
        enricher = PhoneEnricher()
        assert enricher.base_url == "https://phone.example.invalid"


# ---------------------------------------------------------------------------
# DeviceEnricher
# ---------------------------------------------------------------------------


class TestDeviceEnricher:
    def test_returns_device_enrichment(self):
        enricher = DeviceEnricher()
        result = enricher.enrich("fp_abc123")

        assert isinstance(result, DeviceEnrichment)
        assert result.fingerprint == "fp_abc123"
        assert result.first_seen_days == 0
        assert result.is_emulator is False

    def test_empty_fingerprint(self):
        enricher = DeviceEnricher()
        result = enricher.enrich("")

        assert result.fingerprint == ""
        assert result.first_seen_days == 0
        assert result.is_emulator is False

    def test_custom_base_url(self):
        enricher = DeviceEnricher(base_url="https://device.test")
        assert enricher.base_url == "https://device.test"

    def test_default_base_url(self):
        enricher = DeviceEnricher()
        assert enricher.base_url == "https://device.example.invalid"


# ---------------------------------------------------------------------------
# ExtendedPipeline
# ---------------------------------------------------------------------------


class TestExtendedPipeline:
    def test_run_with_all_enrichments(self):
        pipeline = ExtendedPipeline()
        record = _make_record()
        result = pipeline.run(
            record,
            raw_address="123 Main St, Springfield, IL 62704",
            raw_phone="5558675309",
            device_fingerprint="fp_xyz",
        )

        assert result["customer_id"] == "cust_test123"
        assert isinstance(result["address"], AddressEnrichment)
        assert result["address"].normalized_line1 == "123 MAIN ST"
        assert isinstance(result["phone"], PhoneEnrichment)
        assert result["phone"].e164 == "+15558675309"
        assert isinstance(result["device"], DeviceEnrichment)
        assert result["device"].fingerprint == "fp_xyz"

    def test_run_with_no_enrichments(self):
        pipeline = ExtendedPipeline()
        record = _make_record(customer_id="cust_empty")
        result = pipeline.run(record)

        assert result == {"customer_id": "cust_empty"}
        assert "address" not in result
        assert "phone" not in result
        assert "device" not in result

    def test_run_with_address_only(self):
        pipeline = ExtendedPipeline()
        record = _make_record()
        result = pipeline.run(record, raw_address="1 Elm St, Dallas, TX 75201")

        assert "address" in result
        assert "phone" not in result
        assert "device" not in result
        assert result["address"].normalized_city == "DALLAS"

    def test_run_with_phone_only(self):
        pipeline = ExtendedPipeline()
        record = _make_record()
        result = pipeline.run(record, raw_phone="18005551234")

        assert "phone" in result
        assert "address" not in result
        assert "device" not in result
        assert result["phone"].e164 == "+18005551234"

    def test_run_with_device_only(self):
        pipeline = ExtendedPipeline()
        record = _make_record()
        result = pipeline.run(record, device_fingerprint="fp_solo")

        assert "device" in result
        assert "address" not in result
        assert "phone" not in result
        assert result["device"].fingerprint == "fp_solo"

    def test_default_enrichers_initialized(self):
        pipeline = ExtendedPipeline()

        assert isinstance(pipeline.address, AddressEnricher)
        assert isinstance(pipeline.phone, PhoneEnricher)
        assert isinstance(pipeline.device, DeviceEnricher)

    def test_custom_enrichers_injected(self):
        addr = AddressEnricher(base_url="https://addr.test")
        phone = PhoneEnricher(base_url="https://phone.test")
        device = DeviceEnricher(base_url="https://device.test")
        pipeline = ExtendedPipeline(address=addr, phone=phone, device=device)

        assert pipeline.address is addr
        assert pipeline.phone is phone
        assert pipeline.device is device

    def test_none_values_skipped(self):
        pipeline = ExtendedPipeline()
        record = _make_record()
        result = pipeline.run(
            record,
            raw_address=None,
            raw_phone=None,
            device_fingerprint=None,
        )

        assert result == {"customer_id": record.customer_id}

    def test_empty_string_address_skipped(self):
        pipeline = ExtendedPipeline()
        record = _make_record()
        result = pipeline.run(record, raw_address="", raw_phone="", device_fingerprint="")

        assert "address" not in result
        assert "phone" not in result
        assert "device" not in result
