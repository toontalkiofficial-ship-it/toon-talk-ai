# Toon Talk AI — Public Release Readiness

This checklist separates repository implementation from production configuration and real-device/Play verification. A green debug build alone is not a public-release sign-off.

## Repository implementation completed

- [x] Authenticated Supabase account flow: registration, sign-in, session refresh, logout, account deletion UI.
- [x] Per-user backend authorization for generation.
- [x] Server-side Pollinations credential handling; no shared provider secret in the APK.
- [x] Model allowlisting for the configured image/video choices.
- [x] Server-side credit reservation, settlement, refund handling, and idempotency.
- [x] Server-side rate/concurrency protection code.
- [x] Image/video playback and save-to-phone UI.
- [x] Google Mobile Ads banner integration with test IDs for development.
- [x] GitHub Actions debug APK workflow.
- [x] GitHub Actions signed Play Store AAB workflow.
- [x] Android target/compile configuration updated for the current Play release target.

## Still required before public launch

- [ ] **Production backend deployment:** verify the latest Supabase migrations and deploy the `generate` and `account-delete` Edge Functions in the production project.
- [ ] **AI provider:** configure a valid `POLLINATIONS_API_KEY`; verify all enabled model IDs, response formats, pricing, rate limits, and terms with real requests.
- [ ] **Credit pricing:** set verified `MODEL_CREDIT_COSTS_JSON` values that cover actual provider cost plus the platform's intended margin; set production spending/rate limits.
- [ ] **Generated media acceptance:** test image/video success, provider failure, timeout, slow network, repeated taps, stale-output clearing, playback, and save behavior on real Android devices. Durable async video processing is recommended before high-volume public scale.
- [ ] **Ads:** create production AdMob App/Unit IDs, add `ADMOB_APP_ID` and `ADMOB_BANNER_ID` as CI secrets, and complete the applicable consent/privacy flow. Test IDs must not be used for release.
- [ ] **Payments:** if selling credits/features, implement Google Play Billing as required and verify purchase tokens server-side before granting credits. Do not enable paid credits until this is complete.
- [ ] **Privacy/safety:** add a monitored support contact, publish `PRIVACY_POLICY.md` at a stable public URL, and verify disclosures, retention, account deletion, AI-provider handling, advertising, analytics, and content restrictions against the final build.
- [ ] **Release signing:** add the release keystore and signing secrets to GitHub Actions, build the signed AAB, and verify the artifact before upload.
- [ ] **Play Console:** complete internal/closed testing, Data Safety, content rating, app access/account information, privacy-policy URL, store listing, and other required declarations.
- [ ] **Crash/error monitoring:** add and verify a production crash/error monitoring solution before broad public rollout.
- [ ] **Device acceptance:** fresh install, account isolation, invalid/expired session, insufficient balance, offline/slow network, image/video success/failure, rotation/process death, repeated taps, media save/playback.

## Important boundary

Repository code and workflow configuration can be completed here, but production credentials, Supabase deployment, AdMob account setup, payment configuration, signing-key secrets, physical-device verification, and Play Console submission require access/actions outside the repository. Never commit those secrets to source control.
