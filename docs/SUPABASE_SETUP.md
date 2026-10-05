# Supabase setup and deployment — Toon Talk AI

Supabase is used for Android email authentication, user profiles, credit ledger, and the secure generation endpoint. AdMob production setup remains intentionally deferred.

## Already configured
- The project URL and publishable key are saved as GitHub Actions repository secrets named `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`.
- The base tables `profiles`, `credit_ledger`, and `generation_jobs` have been created in the Supabase project.
- Android account sign-up/sign-in, automatic session refresh, credit balance display, account deletion client, and the secure `generate` Edge Function code are in the repository.

## Required deployment steps

### 1. Apply the credit RPC migration once
In Supabase Dashboard → SQL Editor, open a new query and paste the complete contents of:
`supabase/migrations/202610050001_generation_credit_rpcs.sql`
from this repository. Run it once. It creates the service-role-only `reserve_generation` and `finish_generation` functions. Do not rerun the older table-creation SQL; those tables already exist.

### 2. Configure Edge Function secrets
In Supabase Dashboard → Edge Functions → Secrets, add:
- `POLLINATIONS_API_KEY`: your Pollinations secret key beginning with `sk_`. Keep it in Supabase only; never add it to Android, GitHub source, screenshots, or chat.
- `MODEL_CREDIT_COSTS_JSON`: a JSON object mapping each exact `kind:model-id` to a positive integer credit cost, for example the *shape only* `{"image:flux": 10, "video:bytedance/seedance-1-pro-fast": 50}`. These numbers are illustrative, not verified prices. Check current provider pricing and choose costs that cover actual spend before enabling public generation.

Supabase supplies `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` to Edge Functions automatically. Never copy the service-role key into GitHub Actions or the Android app.

### 3. Deploy the Edge Functions
The sources are `supabase/functions/generate/index.ts` and `supabase/functions/account-delete/index.ts`. Deploy both to the same Supabase project. If using the Dashboard editor, create a function named exactly `generate` and deploy the full file contents. If deploying with Supabase CLI, run from the repository root:
`supabase functions deploy generate --project-ref YOUR_PROJECT_REF` and `supabase functions deploy account-delete --project-ref YOUR_PROJECT_REF`
Do not put secrets in source code. Confirm the deployed function uses the secrets above.

The existing `me` function source is `supabase/functions/me/index.ts`; it can be deployed separately if you want the profile/balance endpoint, but the current Android generator calls `generate`.

### 4. Grant test credits safely
New accounts currently receive **zero credits by design**; no welcome grant or payment system is implemented. Before a controlled test, use the Supabase SQL Editor to identify your own test account's UUID in Authentication → Users and add a one-time test ledger entry for that exact UUID. Do not expose any client-side policy that lets users write their own ledger rows. Remove/expire test grants before public release.

### 5. Test before public release
- Create a test account in the Android app and confirm email if required by Auth settings.
- Confirm sign-in succeeds and the app receives an access token.
- Confirm a zero-credit generation returns an insufficient-credit response rather than calling the provider.
- Add a small controlled test credit grant and test one inexpensive image first.
- Verify failed provider calls refund the reservation and successful calls charge the configured credits.
- Test video separately; the current endpoint is synchronous and can time out on long provider jobs.

## Not launch-ready yet
- Credit costs must be verified against current Pollinations prices and adjusted to include a safety margin.
- No verified payment flow / Google Play Billing credit purchase, welcome grant, durable async video jobs, durable background video processing, or production AdMob IDs. Server-side per-user generation rate/concurrency limits are now implemented in the latest migration. Account deletion UI and automatic session refresh are now implemented in the Android client, but the `account-delete` function must be deployed.
- No signed release APK/AAB, privacy-policy publication, or Play Console launch validation.
- The current server-side provider key is a shared app expense. Set a strict budget/monitoring policy before inviting public users.

Never put a Supabase service-role key or Pollinations `sk_` key in Android code, repository files, screenshots, or chat.
