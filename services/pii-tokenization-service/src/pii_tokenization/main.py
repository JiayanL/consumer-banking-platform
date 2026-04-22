"""FastAPI entrypoint for pii-tokenization-service."""
from __future__ import annotations

from fastapi import Depends, FastAPI, HTTPException, status

from . import service
from .auth import require_service_token
from .models import (
    BulkTokenizeRequest,
    BulkTokenizeResponse,
    DetokenizeRequest,
    DetokenizeResponse,
    TokenizeRequest,
    TokenizeResponse,
)

app = FastAPI(title="pii-tokenization-service", version="0.1.0")


@app.get("/healthz")
def healthz() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/tokenize", response_model=TokenizeResponse)
def tokenize(req: TokenizeRequest) -> TokenizeResponse:
    token = service.tokenize(req.field, req.value)
    return TokenizeResponse(token=token, field=req.field)


@app.post("/tokenize/bulk", response_model=BulkTokenizeResponse)
def tokenize_bulk(req: BulkTokenizeRequest) -> BulkTokenizeResponse:
    tokens = [service.tokenize(req.field, v) for v in req.values]
    return BulkTokenizeResponse(tokens=tokens, field=req.field)


@app.post("/detokenize", response_model=DetokenizeResponse)
def detokenize(
    req: DetokenizeRequest,
    _service_token: str = Depends(require_service_token),
) -> DetokenizeResponse:
    value = service.detokenize(req.token)
    if value is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="unknown token")
    return DetokenizeResponse(value=value)


@app.get("/fingerprint/{field}/{value}")
def fingerprint(field: str, value: str) -> dict[str, str]:
    return {"fingerprint": service.fingerprint(field, value)}
