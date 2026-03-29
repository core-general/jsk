# Facebook Core

Facebook login/authentication service — validates Facebook access tokens server-side.

## What It Solves

- `OutFbLoginService` validates a Facebook access token and user ID via facebook4j, returns `OutSimpleUserInfo` (id + name)

## Key Details

- Single-class module
- Uses `FacebookFactory` with empty app credentials (works because it uses user access tokens directly)
