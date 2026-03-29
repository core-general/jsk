# JX Test With Services

Test base class with pre-configured `@Spy` instances for all core JSK services.

## What It Solves

- `MockitoTestWithServices` extends `JskMockitoTest`, adds `@Spy` for `ITime`, `IJson`, `IIds`, `IRand`, `IBytes`, `IAsync`, `IHttp`, etc.
- All services are real implementations (spied, not mocked) — tests run with real behavior but can verify interactions

## Key Details

- Second tier testing — uses `CoreServicesRaw` internally for a full service stack without Spring context
- Prefer this over `JskSpringTest` when you don't need Spring DI
