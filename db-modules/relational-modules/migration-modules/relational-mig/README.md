# Relational Migration

Database migration runner built on Flyway with a flexible JSON-based configuration model.

## What It Solves

- `MigratorBase` — runs Flyway migrations against a database connection with configurable migration paths
- `MigratorModel` — migration configuration model (connection info, migration locations, clean/repair flags)

## Key Details

- Thin wrapper over Flyway providing JSK-style configuration
- Used by applications to run schema migrations at startup or as standalone CLI tools
