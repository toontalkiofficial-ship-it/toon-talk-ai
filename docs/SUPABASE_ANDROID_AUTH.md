# Android Supabase Auth setup

The Android client uses Supabase email/password Auth. It does not use a service-role key.

## Add build-time configuration to GitHub Actions

In the GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret** and create:

- `SUPABASE_URL`: the Project URL shown in Supabase Project Settings.
- `SUPABASE_PUBLISHABLE_KEY`: the Publishable key shown in Supabase API Keys.

Use the publishable key only. Never add a `service_role` or secret key to Android, Gradle properties committed to Git, screenshots, or chat.

The build workflow reads these two repository secrets and supplies them as Gradle project properties. If the secrets are missing, the APK still compiles but the account buttons stay disabled and the screen explains that configuration is missing.

## Supabase Auth settings

- Email provider must be enabled.
- Public signups may stay enabled.
- With email confirmation enabled, a new user must confirm their email before password sign-in returns a session.
- Configure production SMTP and the allowed redirect URLs before public launch.

## Current scope and known limitations

This change adds sign-up, sign-in, password reset request, local session storage, sign-out, and a required account screen before opening the generator.

This is not yet a complete public AI-generation service. The generator still calls Pollinations directly using a user-supplied provider key. The app does not yet have server-side generation, verified credit charging/refunds, account deletion, token refresh, purchase verification, production ads, or a published privacy policy. Do not claim the app is production-ready until those are implemented and tested.
