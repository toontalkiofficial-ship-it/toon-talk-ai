# Supabase backend setup — Toon Talk AI

## Decision and scope
Supabase is selected for managed authentication, PostgreSQL, row-level security (RLS), and Edge Functions. This commit is backend foundation code, not a deployed backend.

## Included
- `supabase/migrations/202610040001_public_backend.sql`: profiles, credit ledger, generation-job table, ownership RLS policies, auth-user profile trigger, and read-only balance function.
- `supabase/functions/me/index.ts`: authenticated profile/balance endpoint.
- No production AdMob configuration is changed.

## Account setup you must do
1. Create a project at https://supabase.com/dashboard.
2. Open the project's SQL Editor and run the migration file from this repository.
3. In Project Settings → API, note the Project URL and publishable/anon key. These may be used by a mobile client; never put the service_role key in Android, GitHub, screenshots, or chat.
4. Enable Email auth in Authentication → Providers and configure email confirmation/SMTP before public release.
5. Deploy the me Edge Function and test it with a real authenticated session.
6. Do not distribute a public build until account deletion, rate limiting, server-only provider credentials, billing verification, and end-to-end tests are complete.

## Important security and product rules
- Client-visible balance is informational only. The server must calculate and mutate balances.
- No welcome credits are granted by this migration. Set a grant only after provider costs and abuse controls are known.
- The current Android app still calls Pollinations directly with a user-supplied API key. This branch does not silently replace that working flow; moving generation to a server requires a verified provider API contract and a secret configured in Supabase.
- A database table is not a payment system. Google Play purchase verification and idempotent credit grants remain separate work.
- media_url is not treated as public until a storage/access design exists.
- AdMob remains deferred; current test IDs do not earn revenue.

## Next stages
1. Connect Android to Supabase Auth using Project URL + publishable/anon key (never service_role).
2. Add sign-up, sign-in, sign-out, password reset and delete-account UX.
3. Add server-side generation jobs, verified model prices, atomic credit reservation/refunds, quotas and provider-secret handling.
4. Add purchase verification, then production ads later.
5. Publish the privacy policy and complete real-device / Play release checks.
