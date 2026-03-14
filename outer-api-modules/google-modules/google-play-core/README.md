# Google Play Core

Google Play integration — login verification (OAuth, ID token, Games API) and in-app purchase/subscription validation.

## What It Solves

- Three login verification methods: server-side auth code exchange (Games API), ID token verification, OAuth2 userinfo endpoint
- `OutGooglePlayPurchaseValidator` validates Google Play purchases and subscriptions using Android Publisher API v3 with service account credentials
- Returns `OutGooglePurchaseResult` with purchase state (OWNED/CONSUMED/BAD)

## Key Details

- Service account auth uses a JSON key file passed as string
- Supports both one-time purchases and subscriptions
- ID token verification uses JSK's `ITime` as clock source
