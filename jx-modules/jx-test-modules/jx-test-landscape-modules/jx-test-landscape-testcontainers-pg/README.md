# JX Test Landscape Testcontainers PostgreSQL

PostgreSQL Testcontainer for the landscape framework — manages a PostgreSQL Docker container with JDBC access, multi-database support, and data cleanup utilities.

## What It Solves

- `JskLandPg` manages a PostgreSQL Testcontainer with configurable Docker image (default `postgres:16.3`)
- Creates `DataSource` and `NamedParameterJdbcOperations` per database
- `JskLandPgWithData` extends landscape with state management: `toEmptyState()` truncates all non-system tables
- Pluggable data seeding via `JskLandPgStateChanger`

## Key Details

- Supports multiple databases within one container (`getSql(databaseName)` auto-creates databases)
- Table cleanup uses `SET session_replication_role = replica` to bypass foreign key constraints
