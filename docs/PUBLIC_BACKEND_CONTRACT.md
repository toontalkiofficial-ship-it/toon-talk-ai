# Public backend contract for Toon Talk AI

This document defines the minimum server contract required before public accounts, credits, or shared AI access are safe. It is a design specification, not a running backend.

## Required components

- Authentication provider: email/password or OAuth, verified sessions, logout, account deletion, and account isolation.
- Database: user profile, credit ledger, generation jobs, provider usage/cost, and idempotency keys.
- Server-only AI credentials: provider keys must stay in server secret storage and never be compiled into the APK.
- Rate limits and abuse controls: per-user and per-IP limits, prompt/content policy, concurrency caps, and spending ceilings.
- Payments: use Google Play Billing for digital features sold inside a Play-distributed Android app where required; verify purchase tokens server-side before granting credits.

## Suggested endpoints

All endpoints except health checks require a verified user session.

- `GET /v1/me`: returns user ID, display name, current server-calculated credit balance, and account status.
- `POST /v1/generations`: accepts `{ "kind": "image" | "video", "modelId": "...", "prompt": "...", "idempotencyKey": "..." }`. The server validates the model and prompt, calculates cost from its own price table, reserves credits atomically, and creates a job.
- `GET /v1/generations/{id}`: returns status, safe error details, and the generated media URL when ready.
- `POST /v1/generations/{id}/cancel`: best-effort cancellation and credit reconciliation.
- `GET /v1/credits/ledger`: returns the authenticated user's own credit transactions.
- `POST /v1/account/delete`: verifies the request and deletes or schedules deletion of personal data according to the published retention policy.
- `POST /v1/billing/google-play/verify`: accepts a purchase token; the server verifies it with Google before a unique, idempotent credit grant.

## Financial correctness requirements

1. Client-supplied balances and prices are never trusted.
2. Each request has an idempotency key; retries cannot double-charge or double-credit.
3. Reserve credits before expensive work; settle actual provider cost after success.
4. Failed/cancelled requests follow a documented refund policy.
5. The ledger is append-only or otherwise auditable; concurrent requests use database transactions/locking.
6. Model pricing comes from verified current provider costs plus the developer's disclosed margin. Do not hard-code guessed prices into the client.
7. The server enforces quotas, maximum prompt size, model allowlists, timeouts, and per-user spending limits.

## Not yet implemented

No live backend URL, database, authentication project, server-side provider secret, verified provider price table, or Google Play Billing setup has been configured for this repository. The Android app must not display these features as active until the server and end-to-end tests exist.
