# JPA Spring PostgreSQL Core

PostgreSQL-specific defaults for JPA configuration.

## What It Solves

- `RdbPostgresProperties` — interface providing PostgreSQL defaults (port 5432, PostgreSQL dialect)
- Extends `RdbProperties` with PostgreSQL-specific conventions

## Key Details

- Essentially a marker/defaults interface — 1-2 files
- Used as a base for PostgreSQL-backed application configurations
