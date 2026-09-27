# Firebase + Google Sign-In setup

This repository now includes a Google Credential Manager → Firebase Authentication sign-in helper and Firebase Auth/Firestore dependencies. It does **not** create cloud resources automatically: Firebase project ownership, OAuth consent, app registration, and rules deployment require an administrator to perform the one-time setup below.

## Google free-tier services used

- **Firebase Authentication**: Google account identity.
- **Cloud Firestore**: device/session metadata and signaling documents (subject to current free quota and region eligibility).
- **Firebase Cloud Messaging (FCM)**: optional wake-up/session notification; add the FCM SDK and notification handling when implementing this path.
- **WebRTC peer-to-peer**: media and input data channels can flow directly between devices when network topology permits. Google’s public STUN endpoint can help discover network addresses, but it is not a relay service and has no availability/SLA commitment.

Free quotas and eligibility change; check the Firebase pricing page before deployment. A TURN relay is often necessary on mobile/carrier/NAT networks. Do not assume a free, reliable, globally available TURN service. Without TURN, some connections will fail.

## One-time console configuration

1. Create a Firebase project at https://console.firebase.google.com/ and add an Android app with application ID `com.aistudio.droidcommand.ultimate`.
2. Download the generated `google-services.json` and place it in `app/google-services.json`. Do not commit service-account private keys or OAuth client secrets.
3. In **Authentication → Sign-in method**, enable Google. Configure the OAuth consent screen and add the SHA-1/SHA-256 fingerprints for the debug and release signing certificates.
4. Obtain the **Web application OAuth client ID** (not the Android client ID). Supply it to `GoogleFirebaseAuth.signIn(activity, webClientId)`; the standard Firebase configuration exposes it as `default_web_client_id` when configured correctly.
5. Create a Cloud Firestore database. Review and deploy the repository's `firestore.rules`. The rules are a baseline for private user/session metadata, not a complete production signaling authorization design.
6. Enable App Check and configure the selected provider for release builds. Keep debug App Check tokens out of source control.
7. Add Firebase Cloud Messaging only if notifications are required. FCM is a wake-up/notification channel, not a remote-control transport.

## Required security architecture before real remote sessions

- Use short-lived, one-time pairing codes; bind each session to the authenticated owner and explicitly approved operator.
- Enforce authorization in trusted backend logic, not only in Firestore client rules. Validate session membership, expiry, replay protection, rate limits, and device revocation.
- Store only minimal session metadata in Firestore. Do not write screen images, typed text, passwords, or accessibility-tree contents to cloud storage.
- Use WebRTC DTLS-SRTP for media and authenticated data channels for input. Validate every incoming command against the active consented session.
- Require the device user to accept screen capture through Android's MediaProjection prompt and to enable Accessibility manually in Android Settings. Keep an ongoing notification and a one-tap stop control. Revoke the session immediately when consent ends.
- Use a TURN service when direct peer-to-peer connectivity fails. Configure credentials to be short-lived; never ship static TURN secrets in the APK.
- Add abuse controls, audit events (without sensitive payloads), privacy disclosures, data retention/deletion, and security tests.

## Important limitation

Google Sign-In authenticates a person; it does not grant Android Device Owner, Accessibility, or screen-capture privileges. Android requires its own user-mediated or enterprise provisioning flows. Firebase does not automatically provision a TURN relay or guarantee peer-to-peer connectivity.
