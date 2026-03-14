# AWS ECS Core

ECS/ECR deployment tooling — builds Docker images, pushes to ECR, and deploys ECS services programmatically.

## What It Solves

- `EcsJskDeployer` builds Docker images, tags with version info, pushes to ECR, and triggers ECS service updates
- `EcsEcrJskClient` wraps AWS ECS and ECR SDK clients for service management and registry auth
- Generates version-tagged images with embedded build metadata

## Key Details

- Uses Spotify's Docker client library for local Docker operations
- Designed for CI/CD pipelines, supports both ECS service update and standalone ECR push workflows
