# Toon Talk AI

Android app for creating cartoon images and short AI videos through a server-managed Pollinations integration.

## Current implementation
- Kotlin Android app with account registration/sign-in, session refresh, logout, and account deletion.
- Supabase is used for authentication, per-user profiles, credit ledger, generation jobs, and the authenticated generation endpoint.
- Image and video requests are sent to the Toon Talk AI Supabase Edge Function; the shared Pollinations provider credential is kept server-side and is never compiled into the APK.
- Image model choices currently include Flux Schnell, Flux, Flux 2 Pro, and GPT Image 1.5.
- Video model choices currently include Seedance 1 Pro Fast, Veo 3.1 Fast, and Seedance 2.0.
- Server-side credit reservation/settlement/refund and idempotency are implemented, with model allowlisting and rate/concurrency protections in the repository.
- Generated image/video playback and save-to-phone are implemented in the Android UI.
- Google Mobile Ads banner integration is present. The repository defaults to Google's test IDs; test ads **do not earn revenue**.
- Release AAB signing workflow is present and expects the release keystore and production configuration through GitHub Actions secrets.

## Build APK
GitHub Actions builds a debug APK on pushes to `main`. Open the repository's **Actions** tab, open the latest successful **Build Toon Talk AI APK** run, and download the `Toon-Talk-AI-APK` artifact.

## Privacy policy
`PRIVACY_POLICY.md` is a draft and must be reviewed, given a monitored support contact, and published at a stable public URL before public release.

## What still requires production setup or verification
1. Configure and verify the Supabase Edge Functions, migrations, `POLLINATIONS_API_KEY`, and exact `MODEL_CREDIT_COSTS_JSON` values in the production Supabase project.
2. Verify current Pollinations model names, pricing, authentication, response formats, rate limits, and terms with real authorized requests.
3. Create production AdMob App/Unit IDs and configure `ADMOB_APP_ID` and `ADMOB_BANNER_ID`; complete the required consent/privacy configuration. Never publish with test IDs.
4. If users will buy credits/features, implement Google Play Billing and server-side purchase-token verification before selling digital goods.
5. Run real-device acceptance tests for authentication, insufficient credits, offline/slow network, image/video success/failure, repeated taps, process death, saving media, and account isolation.
6. Configure the release keystore secrets, build the signed AAB, test it through Play Console, and complete the required Play listing/Data Safety/content declarations.

## Security
Never commit Pollinations, Supabase service-role, AdMob, payment, or signing secrets to the repository. The Android app only receives the Supabase publishable key and public configuration.

## Important
AI generation depends on provider availability, enabled model support, configured credits, rate limits, and the final production configuration.