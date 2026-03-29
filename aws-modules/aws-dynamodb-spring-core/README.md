# AWS DynamoDB Spring Core

Spring configuration that wires DynamoDB components (`DynClient`, `DynConfigurator`, optionally `IKvUnlimitedStore`) as Spring beans.

## What It Solves

- `DynBeanConfig` registers `DynClient` and `DynConfigurator` beans
- `DynBeanConfigWithKvStore` extends above, adds `DynKVStoreImpl` as an `IKvUnlimitedStore` bean

## Key Details

- Very thin bridge — just bean declarations, no logic
- Depends on `aws-dynamodb-core` and `spring-core`
