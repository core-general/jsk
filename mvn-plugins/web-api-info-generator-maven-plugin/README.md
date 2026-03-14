# Web API Info Generator Maven Plugin

Maven plugin that parses JSK web API interfaces at build time — extracts method signatures, paths, Javadoc, parameter info, and generates metadata JSON + git version info.

## What It Solves

- Uses JavaParser to parse `@WebPath`-annotated API interfaces
- Extracts method names, URL paths, parameters (including `@WebParamsToObject`), return types, and full Javadoc
- Recursively resolves DTO types (fields, parent classes, enum entries) referenced in API methods
- Generates `ApiBuildInfo` with git hash + commit count + build timestamp

## Key Details

- Goal prefix is `webapi`
- Mandatory for any module using JSK web framework — without it, `WebMethodInfoProviderImpl` throws at startup
- Computes per-method hashes for API change detection
