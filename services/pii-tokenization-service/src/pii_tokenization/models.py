from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel


PiiField = Literal["ssn", "dob", "account_number"]


class TokenizeRequest(BaseModel):
    field: PiiField
    value: str


class TokenizeResponse(BaseModel):
    token: str
    field: PiiField


class DetokenizeRequest(BaseModel):
    token: str


class DetokenizeResponse(BaseModel):
    value: str


class BulkTokenizeRequest(BaseModel):
    field: PiiField
    values: list[str]


class BulkTokenizeResponse(BaseModel):
    tokens: list[str]
    field: PiiField
    errors: Optional[list[str]] = None
