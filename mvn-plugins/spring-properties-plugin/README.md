# Spring Properties Plugin

Maven plugin that scans compiled classes for Spring property files and saves their metadata for runtime discovery.

## What It Solves

- Goal `CREATE_META` (phase `PREPARE_PACKAGE`) scans compile output for `.properties` files under a configured prefix
- Calls `PropertyMetaService.savePropertyNames()` to persist metadata as JSON

## Key Details

- Thread-safe (uses `JLockDecorator`)
- Produces output consumed by `PropertyMetaService.getPropertyNames()` at runtime
- Works in tandem with `spring-properties-plugin-model`
