"""Extended enrichment pipeline.

The core ``KycService.enrich`` only runs sanctions + PEP. Downstream
product teams asked for additional enrichment stages (address, phone,
device fingerprint) that aren't yet wired into the main flow — they
live here so the plumbing can be code-reviewed ahead of time.

None of this is active in production yet; it's feature-flagged off.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Optional

from .models import EnrichedRecord


@dataclass
class AddressEnrichment:
    normalized_line1: str
    normalized_city: str
    normalized_state: str
    normalized_postal: str
    usps_deliverable: bool


@dataclass
class PhoneEnrichment:
    e164: str
    carrier: Optional[str]
    line_type: str  # mobile | landline | voip | unknown


@dataclass
class DeviceEnrichment:
    fingerprint: str
    first_seen_days: int
    is_emulator: bool


class AddressEnricher:
    """Normalizes and validates postal addresses via the address vendor."""

    def __init__(self, base_url: str = "https://addr.example.invalid") -> None:
        self.base_url = base_url

    def enrich(self, raw_address: str) -> AddressEnrichment:
        parts = [p.strip() for p in raw_address.split(",")]
        line1 = parts[0] if parts else ""
        city = parts[1] if len(parts) > 1 else ""
        state = parts[2].split()[0] if len(parts) > 2 else ""
        postal = parts[2].split()[-1] if len(parts) > 2 else ""
        return AddressEnrichment(
            normalized_line1=line1.upper(),
            normalized_city=city.upper(),
            normalized_state=state.upper(),
            normalized_postal=postal,
            usps_deliverable=True,
        )


class PhoneEnricher:
    def __init__(self, base_url: str = "https://phone.example.invalid") -> None:
        self.base_url = base_url

    def enrich(self, raw_phone: str) -> PhoneEnrichment:
        digits = "".join(c for c in raw_phone if c.isdigit())
        if len(digits) == 10:
            e164 = f"+1{digits}"
        elif len(digits) == 11 and digits.startswith("1"):
            e164 = f"+{digits}"
        else:
            e164 = raw_phone
        return PhoneEnrichment(
            e164=e164,
            carrier=None,
            line_type="unknown",
        )


class DeviceEnricher:
    def __init__(self, base_url: str = "https://device.example.invalid") -> None:
        self.base_url = base_url

    def enrich(self, fingerprint: str) -> DeviceEnrichment:
        return DeviceEnrichment(
            fingerprint=fingerprint,
            first_seen_days=0,
            is_emulator=False,
        )


class ExtendedPipeline:
    """Runs address/phone/device enrichment after the core flow.

    Intentionally not called from ``main.py`` yet — the product launch
    is gated behind the ``kyc.extended_enrichment`` feature flag.
    """

    def __init__(
        self,
        address: Optional[AddressEnricher] = None,
        phone: Optional[PhoneEnricher] = None,
        device: Optional[DeviceEnricher] = None,
    ) -> None:
        self.address = address or AddressEnricher()
        self.phone = phone or PhoneEnricher()
        self.device = device or DeviceEnricher()

    def run(
        self,
        record: EnrichedRecord,
        *,
        raw_address: Optional[str] = None,
        raw_phone: Optional[str] = None,
        device_fingerprint: Optional[str] = None,
    ) -> dict:
        result: dict = {"customer_id": record.customer_id}
        if raw_address:
            result["address"] = self.address.enrich(raw_address)
        if raw_phone:
            result["phone"] = self.phone.enrich(raw_phone)
        if device_fingerprint:
            result["device"] = self.device.enrich(device_fingerprint)
        return result
