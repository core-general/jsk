# Web Client Core Java

Java SE implementation of `WebPlatformSpecificHelper` using `java.lang.reflect.Proxy` for runtime API client generation.

## What It Solves

- `WebJavaSpecificHelper` creates dynamic proxies via `Re.singleProxy()`, handles `Object` methods gracefully

## Key Details

- Single-class module
- Only needed for standard JVM (not TeaVM)
