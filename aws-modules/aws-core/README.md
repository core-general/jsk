# AWS Core

Foundation module for all AWS integrations in JSK. Provides credential management, region configuration, and a generic AWS SDK v2 client factory.

## What It Solves

- Generic factory (`AwsUtilityHelper`) that creates AWS SDK v2 sync/async clients with proper credential and region setup
- Unified `AwsProperties` contract for credentials, region, and URI addressing
- Supports both standard AWS regions and custom S3-compatible endpoints (e.g., MinIO) via `OneOf<URI, Region>` pattern
- Resolves current EC2 instance public IP from metadata service via `AwsIpProvider`

## Key Details

- All other AWS modules depend on this one
- `AwsWithChangedPort` decorator allows overriding ports of existing `AwsProperties` endpoints
