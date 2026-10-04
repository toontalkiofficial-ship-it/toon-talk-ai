# Toon Talk AI

Android app for creating cartoon images and short AI videos through Pollinations.

## Current implementation
- Android project built with Kotlin and Android Gradle Plugin.
- Image generation uses Pollinations `/image/{prompt}`.
- Video generation uses Pollinations `/video/{prompt}` with a 4-second model request.
- User supplies their own authorized Pollinations API key. The key is stored in the app's private preferences on their device; do not put a secret key in source code.
- Google Mobile Ads banner integration is present. The default IDs are Google's test IDs and **do not earn revenue**.

## Build APK
GitHub Actions builds a debug APK on each push to `main`. Open the repository's **Actions** tab, open the latest successful **Build Toon Talk AI APK** run, and download the `Toon-Talk-AI-APK` artifact.

## Privacy policy draft
`PRIVACY_POLICY.md` is a starting draft only. It is not yet a published public policy: add a monitored support email, verify all disclosures against the final build, and publish it at a stable public URL before launch.

## Before a public release
1. Create/register the app's Pollinations App Key and implement its OAuth/BYOP flow if the app should connect users without asking them to paste their own API key. Do not embed a secret `sk_` key in the APK.
2. Create an AdMob app and production ad unit. Set Gradle project properties `ADMOB_APP_ID` and `ADMOB_BANNER_ID` in the build environment. Do not publish with test ad IDs.
3. Publish and link a privacy policy. Review Google Play policies, ad disclosures, content safety, and AI-service terms.
4. Build and test on real Android devices, including generation errors, slow network, low balance, saving media, and video playback.
5. A Play Store release should use a properly signed Android App Bundle (AAB) and Play App Signing. The current CI artifact is a debug APK for testing, not a Play Store release.

## Important
Generation depends on Pollinations availability, model support, user-authorized API key, and sufficient balance. Generated image/video quality and availability cannot be guaranteed for every prompt.
