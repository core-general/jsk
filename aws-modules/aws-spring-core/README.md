# AWS Spring Core

Minimal Spring configuration that registers `AwsUtilityHelper` as a Spring bean.

## What It Solves

- `AwsBeanConfig` — single `@Configuration` class creating `AwsUtilityHelper` bean

## Key Details

- One-class module — pure Spring wiring
- Depends on `aws-core` and `spring-core`
