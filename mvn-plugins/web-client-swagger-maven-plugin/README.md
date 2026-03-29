# Web Client Swagger Maven Plugin

Maven plugin that generates OpenAPI/Swagger specs from JSK web API interfaces and optionally generates client SDKs via OpenAPI Generator.

## What It Solves

- Loads API class metadata and generates OpenAPI 3.0 JSON spec via `WebSwaggerGenerator`
- Saves spec to `__jsk_util/swagger/api_specs/`
- Optionally runs OpenAPI Generator to produce client SDKs (e.g., Dart, TypeScript)

## Key Details

- Goal prefix is `swagger`
- Can generate client code in multiple languages in a single build pass
- Depends on `web-client-swagger-generator` and `openapi-generator`
