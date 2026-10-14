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

## Request options

The `createWebApiClient` overloads accepting `WebClientRequestOptions` configure redirects, the total response deadline, maximum response bytes, and an optional connection timeout. Existing overloads retain their defaults.

```java
var options = new WebClientRequestOptions(false, Duration.ofSeconds(3), 1024 * 1024,
        Duration.ofSeconds(2));
var api = factory.createWebApiClient(baseUrl, ExampleApi.class, resultHandler, options).get();
```

The three-argument options constructor retains the HTTP service's connection timeout. A null total timeout retains its usual request timeout; a configured total timeout covers headers and body consumption. Response limits apply before JSON parsing, including decoded gzip content. Input handlers can still add request headers explicitly.
