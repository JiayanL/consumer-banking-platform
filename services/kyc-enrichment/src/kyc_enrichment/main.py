"""FastAPI entrypoint for the KYC enrichment service."""
from __future__ import annotations

from fastapi import FastAPI, HTTPException

from .models import EnrichedRecord, EnrichRequest, StatusResponse
from .service import EnrichmentError, KycService


def create_app(service: KycService | None = None) -> FastAPI:
    app = FastAPI(title="kyc-enrichment", version="0.1.0")
    svc = service or KycService()

    @app.post("/kyc/enrich", response_model=EnrichedRecord, status_code=201)
    def enrich(payload: EnrichRequest) -> EnrichedRecord:
        try:
            return svc.enrich(payload)
        except EnrichmentError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc

    @app.get("/kyc/{customer_id}/status", response_model=StatusResponse)
    def status(customer_id: str) -> StatusResponse:
        try:
            return svc.status(customer_id)
        except KeyError as exc:
            raise HTTPException(status_code=404, detail="customer not found") from exc

    @app.post("/kyc/reverify/{customer_id}", response_model=EnrichedRecord)
    def reverify(customer_id: str) -> EnrichedRecord:
        try:
            return svc.reverify(customer_id)
        except KeyError as exc:
            raise HTTPException(status_code=404, detail="customer not found") from exc

    return app


app = create_app()
