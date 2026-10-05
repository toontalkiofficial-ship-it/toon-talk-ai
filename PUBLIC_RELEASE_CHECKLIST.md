# Toon Talk AI — Public Release Readiness

This checklist tracks work that must be implemented and verified before a public launch. A successful debug APK build is not a public-release sign-off.

## Current code observations

- The Android app now requires an authenticated Supabase account for generation; the shared Pollinations provider key stays server-side in the Supabase Edge Function.
- Image and video generation requests go through the authenticated `generate` Edge Function.
- Google Mobile Ads is integrated with Google's test IDs by default; these do not generate production revenue.
- The current GitHub Actions workflow creates a debug APK, not a signed Play Store release bundle.
- A public multi-user product still needs a deliberate account, billing/credits, and backend design.

## Release blockers and acceptance criteria

- [ ] **AI provider integration:** model selectors and provider-key removal control exist. Verify current Pollinations authentication, model names, endpoint response formats, pricing, rate limits, and terms. Test successful and failed image/video requests against real authorized credentials.
- [ ] **Protect provider credentials:** never embed a shared secret in the APK. For a managed public service, route requests through a backend that stores secrets server-side, authenticates users, enforces quotas, and rate-limits requests. If keeping bring-your-own-key, explain this clearly and never upload the user's key.
- [x] **User accounts:** Android registration/login, session refresh, logout, account deletion UI, and per-user backend authorization are implemented. Verify the deployed `account-delete` function and real-device behavior.
- [x] **Credits and budget limits:** server-side reservations/settlement/refunds/idempotency and model allowlisting are implemented. Still configure verified current provider costs and add production rate limits/spending ceilings.
- [ ] **Generated media:** save-to-phone, image/video playback, stale-output clearing, and server-side charging are implemented. Verify success/failure/slow-network behavior on real devices and add durable async video processing before public scale.
- [ ] **Ads:** production App ID/unit IDs must be created and added as `ADMOB_APP_ID` and `ADMOB_BANNER_ID` CI secrets; consent/privacy flow still needs final verification.
- [ ] **Payments:** choose a payment provider and confirm Google Play Billing requirements for digital goods before implementing purchases. Verify receipts server-side and prevent duplicate crediting.
- [ ] **Privacy and safety:** `PRIVACY_POLICY.md` draft exists, but it is not published. Add a monitored support contact; verify disclosures against the final build; publish at a stable public URL; disclose AI provider/data handling, ads, analytics, retention, account deletion, and content restrictions.
- [ ] **Production release:** add automated tests, crash/error monitoring, release signing secrets, a signed Android App Bundle, and Play Console testing.
- [ ] **Device acceptance test:** verify fresh install, no API key, invalid key, insufficient balance, offline/slow network, image success/failure, video success/failure, repeated taps, rotation/process death, and account isolation.

## Safe implementation order

1. Build and install the current debug APK; record actual behavior and errors.
2. Stabilize image/video provider calls and media save/share.
3. Select and implement backend/authentication before introducing shared credits or server-managed keys.
4. Add server-enforced usage limits and pricing.
5. Configure production ads and, only if desired, payments.
6. Complete privacy, security, and release testing before public distribution.

## Configuration needed before production features

Production auth, server-managed AI access, credits, and payments cannot be completed safely from Android-only code. They require selecting/authorizing the backend and AI provider, setting secrets outside source control, and defining the pricing/credit policy. Never commit API keys, signing keys, or payment secrets to the repository.
