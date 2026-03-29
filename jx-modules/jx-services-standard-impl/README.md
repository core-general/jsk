# JX Services Standard Implementation

Standard implementations of all core service interfaces — provides a ready-to-use service stack without any DI framework.

## What It Solves

- `CoreServicesRaw.services()` creates a complete DI-free service stack (all core services wired together)
- Concrete implementations: `AsyncImpl`, `BytesImpl`, `JGsonIJson` (Gson-based JSON), `TimeUtcImpl`, `IdsImpl`, `RandImpl`, `IHttpImpl`

## Key Details

- `CoreServicesRaw` enables using JSK services without Spring or any DI framework
- Useful for CLI tools, tests, and standalone scripts
