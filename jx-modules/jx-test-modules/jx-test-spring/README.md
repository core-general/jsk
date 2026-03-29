# JX Test Spring

Spring integration test base class for tests requiring a full Spring `ApplicationContext`.

## What It Solves

- `JskSpringTest` — base class for Spring-based integration tests with context setup and tear-down

## Key Details

- Third (heaviest) test tier — use only when you need Spring DI
- Prefer `JskMockitoTest` or `MockitoTestWithServices` for faster tests
