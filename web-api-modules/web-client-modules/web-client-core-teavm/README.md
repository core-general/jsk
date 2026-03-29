# Web Client Core TeaVM

TeaVM implementation of `WebPlatformSpecificHelper` for compiling API clients to JavaScript.

## What It Solves

- `WebTeaVMSpecificHelper` — placeholder/framework for TeaVM API client generation
- Uses `Window.encodeURIComponent()` for browser-native URL encoding

## Key Details

- Currently a **stub** — `createClient()` throws `UnsupportedOperationException`
- Requires a TeaVM Metaprogramming plugin or annotation processor to generate concrete client classes
- Depends on `org.teavm:teavm-jso-apis`
