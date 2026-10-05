# Toon Talk AI — Privacy Policy (Draft)

**Last updated:** 5 October 2026

> **Release note:** This is a draft for review, not legal advice. Before public release, replace the support-contact placeholder, publish this policy at a stable public URL, confirm every statement against the final app and SDK configuration, and obtain any required legal review.

## Information the app handles

- **Account information:** The app uses Supabase Authentication for email/password accounts and stores a display name and account status in the app backend. Authentication tokens are stored in private app storage on the device.
- **Prompts:** Prompts are sent through the Toon Talk AI backend to Pollinations to fulfil the requested generation. The shared provider credential is kept server-side and is not placed in the Android app.
- **Credits and generation records:** The backend stores credit ledger entries and generation job metadata needed to enforce balances, prevent duplicate charging, and refund failed generations.
- **Generated media:** Images and videos returned by the provider are displayed in the app. If you choose Save, a copy is written to your device's Pictures/ToonTalkAI or Movies/ToonTalkAI folder.
- **Advertising:** Development builds may use Google's test ad units. Test ad units do not generate publisher revenue. A public release may use Google Mobile Ads only after the production configuration and required consent/disclosure flows are completed. Google may process data as described in its own policies when real ads are enabled.

## Accounts, payments, and analytics

The app provides account registration/sign-in and server-managed credits. The current repository does not yet include a verified in-app credit purchase flow. No first-party analytics account is intentionally configured.

## Sharing and third-party services

Your prompt is sent through the Toon Talk AI backend to Pollinations to fulfil the AI request. The shared provider credential is stored only in the backend environment and is not intentionally exposed to the Android client. Review Pollinations' current privacy policy and terms before using the service. The app developer does not control the provider's independent processing or retention practices. If production advertising is enabled, review Google's privacy disclosures and configure any consent requirements that apply.

## Data security and retention

Authentication tokens are stored in the app's private local preferences. Backend credentials are stored as server-side secrets and are not included in the APK. Generated files saved to shared device media remain there until you delete them. Credit and generation records are retained in the backend according to the final retention policy. Avoid entering sensitive personal information in prompts.

## Children and acceptable use

Parents or guardians should supervise children's use. Do not use the app to create unlawful, abusive, deceptive, or non-consensual content, or content that violates another person's rights. The final public release must include age-rating and content-safety measures appropriate to its intended audience.

## Your choices

You can sign out or use the in-app Delete account action to request deletion of your account and associated server-side profile, credit ledger, and generation records. Delete saved images/videos from your device's gallery or file manager. Any data held by Pollinations or an enabled advertising provider is subject to that provider's processes and policies.

## Contact

Privacy questions and requests: **[Add a monitored support email before publication]**

## Before publication

The developer must verify this policy against the actual release build, add a real support contact, publish it at a stable public URL, and update it for final payment, advertising, retention, and backend configuration before publication.
