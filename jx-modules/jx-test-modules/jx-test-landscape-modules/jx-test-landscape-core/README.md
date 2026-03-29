# JX Test Landscape Core

Test landscape framework — orchestrates multi-component integration test environments with lifecycle management, state transitions, and parallel initialization.

## What It Solves

- `JskLand` — base class for test environment components with lifecycle (`start()`/`stop()`) and thread-safe status tracking
- `JskLandScape` — composite of multiple `JskLand` instances with typed state transitions
- `JskLandScapeParallel` — parallel initialization/shutdown of landscape components using `IAsync`
- `JskFullLand` — top-level landscape that auto-starts on construction and supports graceful shutdown

## Key Details

- Container-agnostic — `JskLand` can wrap any test resource (DB, message queue, external service)
- Includes `JskLandDefaultConfig` for Spring-based landscapes with automatic port management
