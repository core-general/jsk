# AWS CDK Core

High-level AWS CDK abstraction for provisioning complete application infrastructure stacks via Java code. Creates full environments (VPC, EC2, ALB, RDS, Redis) through a single method call.

## What It Solves

- Creates full stacks (VPC, EC2 with Elastic IP, ALB with HTTPS/TLS, RDS PostgreSQL, Redis ElastiCache) via `createAlbApp()`
- Manages security groups, subnets, and cross-resource connectivity rules automatically
- Configurable via `JskGenericStackParams` (domain, allowed IPs, EC2 image, PG version, Redis settings, deletion protection)

## Key Details

- Designed for cost-optimization — uses `t3.micro`, no NAT gateways, single-AZ, public subnets only
- Depends only on `jx-utils` and AWS CDK lib (no Spring, no aws-core)
