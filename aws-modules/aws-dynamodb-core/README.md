# AWS DynamoDB Core

DynamoDB client library providing typed CRUD operations, key-value store abstraction, and automatic table provisioning.

## What It Solves

- `DynClient` wraps AWS SDK DynamoDB client with typed get/put/scan/query/delete via `IJson` serialization
- `DynKVStoreImpl` implements `IKvUnlimitedStore`, enabling DynamoDB as a pluggable KV store
- `DynConfigurator` creates DynamoDB tables on startup if they don't exist
- Automatic created/updated timestamp management via `CreatedAndUpdatedAtExtension`

## Key Details

- `DynKVStoreImpl` implements `IKvUnlimitedStore` — the unlimited variant of the KV store hierarchy
- Table definitions use the `DynTable` interface pattern (not annotations) for compile-time safety
