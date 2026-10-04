# Secure generation endpoint

This is a server-side beta endpoint, not yet deployed. It proxies Pollinations image/video generation so the provider key stays in Supabase Edge Function secrets rather than the APK.

## Required Supabase setup

1. Apply `supabase/migrations/202610050001_generation_credit_rpcs.sql` after the existing public backend migration.
2. Set Edge Function secrets in the Supabase project (never in Android or GitHub Actions):
   - `POLLINATIONS_API_KEY`: an authorized provider key.
   - `MODEL_CREDIT_COSTS_JSON`: a JSON object mapping every enabled model to a positive integer credit cost. Example shape only: `{"image:flux":10,"video:bytedance/seedance-1-pro-fast":100}`. **These numbers are examples, not actual provider prices.** Set costs only after checking provider pricing and deciding the platform's margin.
3. Deploy the `generate` Edge Function and test using an authenticated account with a funded credit ledger.
4. Keep `SUPABASE_SERVICE_ROLE_KEY` only in Supabase Edge Function secrets. Never add it to GitHub repository secrets or the Android app.

## Request

POST `/functions/v1/generate` with a user access token and JSON:
`{"kind":"image","model":"flux","prompt":"A friendly 3D cartoon fox","idempotencyKey":"unique-request-id-123"}`

Success returns media bytes with `Content-Type`, `X-Generation-Job-Id`, and `X-Credits-Charged` headers. Failures return a safe JSON error and refund the reserved credits when the provider call fails.

## Important limits

- The caller must be authenticated and have sufficient credits; the model list is allowlisted.
- Model costs are mandatory configuration; no unverified pricing or free credits are hard-coded.
- The endpoint is synchronous and has a short timeout to respect Edge Function runtime limits. Video generation may time out; production should move long video work to a durable asynchronous queue with polling.
- The function does not yet implement payment verification, rate limiting, moderation review, media storage, or account deletion. Do not launch publicly until these are completed and tested.
