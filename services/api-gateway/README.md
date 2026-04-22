# api-gateway

Thin public-facing gateway. Validates presence of an `Authorization`
header (signature verification lives downstream), attaches a
request ID to every request, and routes `/api/*` to stubbed downstream
clients. No real network calls are made — the clients return canned
shapes for local dev and contract tests.
