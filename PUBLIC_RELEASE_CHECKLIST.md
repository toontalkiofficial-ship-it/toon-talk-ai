# Toon Talk AI — Public Release Readiness

This checklist tracks work that must be implemented and verified before a public launch. A successful debug APK build is not a public-release sign-off.

## Current code observations

- The Android app currently asks each user to paste a Pollinations API key and stores it in private app preferences.
- Image and video generation are called directly from the Android app.
- Google Mobile Ads is integrated with Google's test IDs by default; these do not generate production revenue.
- The current GitHub Actions workflow creates a debug APK, not a signed Play Store release bundle.
- A public multi-user product still needs a deliberate account, billing/credits, and backend design.

## Release blockers and acceptance criteria

- [ ] **AI provider integration:** model selectors and provider-key removal control exist. Verify current Pollinations authentication, model names, endpoint response formats, pricing, rate limits, and terms. Test successful and failed image/video requests against real authorized credentials.
- [ ] **Protect provider credentials:** never embed a shared secret in the APK. For a managed public service, route requests through a backend that stores secrets server-side, authenticates users, enforces quotas, and rate-limits requests. If keeping bring-your-own-key, explain this clearly and never upload the user's key.
- [ ] **User accounts:** backend contract is documented in `docs/PUBLIC_BACKEND_CONTRACT.md`; still implement secure registration/login, session handling, logout, account deletion, and per-user data isolation with a selected, configured provider.
- [ ] **Credits and budget limits:** backend contract is documented in `docs/PUBLIC_BACKEND_CONTRACT.md`; still define per-model prices from verified provider costs and implement server-side reservations/settlement, refunds, idempotency, concurrency controls, and abuse prevention.
- [ ] **Generated media:** save-to-phone code exists and generation hides stale output before a new request; verify image/video playback, persistent save/share, progress/error states, request cancellation, and handling of large files on real devices.
- [ ] **Ads:** create the production AdMob app and ad units, configure the real App ID and unit IDs in CI secrets/properties, add required privacy/consent flows, and keep test ads during development.
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
