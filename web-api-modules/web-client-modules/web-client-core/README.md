# Web Client Core

Platform-agnostic HTTP client framework that generates type-safe API clients from Java interfaces using proxy-based method interception.

## What It Solves

- `WebClientFactory` — central factory that creates API proxy clients from interface class + base URL + result handler
- `WebPlatformSpecificHelper` — platform abstraction (Java uses dynamic proxies, TeaVM uses compile-time generation)
- Response processing chain for JSON deserialization via `WebClientResultHandlerSimpleJsonImpl`
- Pre-request hooks via `WebClientInputHandler` for modifying execution models

## Key Details

- Depends only on `web-core` (no Spring, no Jetty) — truly portable
- The `WebPlatformSpecificHelper` abstraction exists specifically for TeaVM (which can't use `java.lang.reflect.Proxy`)
