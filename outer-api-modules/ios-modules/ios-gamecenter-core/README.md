# iOS Game Center Core

iOS authentication and purchase validation — Game Center login, Sign in with Apple, App Store receipt verification, and subscription status checking.

## What It Solves

- Game Center player validation via Apple's cryptographic signature (SHA256withRSA)
- Sign in with Apple server-side auth using ES256 JWT client secrets
- App Store receipt verification (production + sandbox fallback on status 21007)
- App Store Server API v2 subscription status queries with JWT-based auth

## Key Details

- Receipt validation auto-falls back to sandbox if production returns 21007
- Uses JWS parsing for Apple's signed transaction payloads
- Depends on `java-jwt` (Auth0)
