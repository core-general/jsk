# JPA Key-Value Store

JPA-backed key-value store implementation — stores arbitrary JSON-serialized values in a relational database table.

## What It Solves

- `RdbKVStoreImpl` — full `IKVStore` implementation backed by JPA entities with QueryDSL predicates
- Supports categories, bulk operations, TTL-style expiration, and optimistic locking
- Provides a portable KV abstraction — same interface as DynamoDB's `DynKVStoreImpl`

## Key Details

- The `raw_value` column is `byte[]` (BYTEA), not TEXT
- TTL support is currently inert (always passes `null`)
