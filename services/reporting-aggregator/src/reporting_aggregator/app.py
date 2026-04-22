"""Flask app entrypoint for reporting-aggregator."""
from __future__ import annotations

from uuid import uuid4

from flask import Flask, jsonify, request

from . import seed
from .aggregations import build_report


def create_app() -> Flask:
    app = Flask("reporting-aggregator")
    _reports: dict[str, dict] = {}

    @app.post("/reports/daily")
    def daily():
        date = request.args.get("date")
        if not date:
            return jsonify({"error": "date query param is required"}), 400
        report_id = f"rep_d_{uuid4().hex[:10]}"
        report = build_report(report_id, seed.for_date(date))
        report["period"] = {"kind": "daily", "date": date}
        _reports[report_id] = report
        return jsonify(report), 201

    @app.post("/reports/monthly")
    def monthly():
        period = request.args.get("period")
        if not period:
            return jsonify({"error": "period query param is required"}), 400
        report_id = f"rep_m_{uuid4().hex[:10]}"
        report = build_report(report_id, seed.for_month(period))
        report["period"] = {"kind": "monthly", "period": period}
        _reports[report_id] = report
        return jsonify(report), 201

    @app.get("/reports/<report_id>")
    def get_report(report_id: str):
        report = _reports.get(report_id)
        if report is None:
            return jsonify({"error": "not found"}), 404
        return jsonify(report), 200

    return app


app = create_app()
