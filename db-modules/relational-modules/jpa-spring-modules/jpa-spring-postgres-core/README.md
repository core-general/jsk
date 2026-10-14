# JPA Spring PostgreSQL Core

PostgreSQL-specific defaults for JPA configuration.

## What It Solves

- `RdbPostgresProperties` — interface providing PostgreSQL defaults (port 5432, PostgreSQL dialect)
- Extends `RdbProperties` with PostgreSQL-specific conventions

## Key Details

- Supplies PostgreSQL configuration defaults
- Used as a base for PostgreSQL-backed application configurations

## Transaction verification

`TransactionCreationRetryTest` uses a fresh PostgreSQL Testcontainer and direct test-scoped Testcontainers dependencies, avoiding
a dependency on the JSK PostgreSQL landscape module that already depends on this module. Forced missing-key races verify
rollback and fresh reads at explicit flush, automatic flush, and deferred database commit. The suite also covers optimistic
updates, joined and independent transactions, exhaustion, exception chains, unrelated constraints, and business failures.
