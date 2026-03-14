# JPA Hibernate Core

Low-level database connectivity layer — HikariCP DataSource creation and connection configuration interfaces.

## What It Solves

- `RdbProperties` — interface defining connection config (driver, URL, user, pass, pool size, JPA packages, dialect)
- `RdbUtil` — creates HikariCP `DataSource` from `RdbProperties`
- `RdbWithChangedPort` — port override interface for test environments

## Key Details

- Foundation for all JPA modules
- Does not depend on Spring — can be used standalone with any connection pool consumer
