# JX Test Landscape Testcontainers

Base Testcontainers integration for the landscape framework — bridges `JskLand` lifecycle with Docker container management.

## What It Solves

- `JskLandContainer<DOCKER>` — abstract `JskLand` subclass that manages a `GenericContainer` lifecycle
- Auto-starts the container on first `getContainer()` call if not already initialized
- Maps `doInit()` → `container.start()` and `doShutdown()` → `container.stop()`

## Key Details

- Single-class module providing the foundation for all specific testcontainer implementations (PostgreSQL, LocalStack)
- The `outsidePort` parameter enables fixed port binding for predictable test configuration
