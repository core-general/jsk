# Web Client Swagger Generator

Generates OpenAPI 3.0 specifications from JSK API interfaces using the same metadata model that server and client use.

## What It Solves

- `WebSwaggerGenerator` introspects API interface methods via `WebClassInfoProvider` and builds full `OpenAPI` spec
- Handles JSK-specific types: `O<T>` → nullable, `IdBase`/`IdString`/`IdLong`/`IdUuid` → string/integer, enums, `byte[]` → binary
- Maps `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime` to date/datetime string formats

## Key Details

- Depends on `io.swagger.parser.v3` and `io.swagger.core.v3`
- Generated spec powers the `/api-info-swagger` endpoint and the Maven plugin for client SDK generation
