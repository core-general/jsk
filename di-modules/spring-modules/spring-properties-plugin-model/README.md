# Spring Properties Plugin Model

Build-time/runtime bridge for Spring property file discovery — stores property metadata as JSON resources during Maven build, loads them at runtime.

## What It Solves

- Holds property path prefix and list of config file names via `PropertyMeta`
- Writes property metadata to `__jsk_util/properties/{module}/props4spring.json` during Maven build
- Discovers all property metadata files from classpath at runtime via `PropertyMetaService.getPropertyNames()`

## Key Details

- Convention-based resource path (`__jsk_util/properties/`) enables cross-module property discovery without explicit wiring
- Does not depend on Spring itself — only on `jx-services-standard-impl`
