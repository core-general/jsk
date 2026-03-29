# JX Test Landscape Testcontainers LocalStack

LocalStack (AWS) Testcontainer for the landscape framework — provides local S3 and DynamoDB services for integration testing.

## What It Solves

- `JskLandLocalstack` manages a LocalStack Testcontainer (S3 + DynamoDB services) with lazy-initialized `S3JskClient` and `DynClient`
- `toEmptyState()` clears all S3 buckets and DynamoDB tables in parallel
- Spring configs with fixed or random port binding

## Key Details

- Uses `localstack/localstack:3.4.0` image
- Detects internet availability and skips SSL cert/infra downloads when offline
- Implements `JskLandEmptyStateMixin` for clean-state testing
