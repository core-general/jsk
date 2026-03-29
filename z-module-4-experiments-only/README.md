# Experiments Module

Integration testing sandbox depending on most JSK modules. Used for end-to-end testing of the web framework, Spring integration, AWS services, PostgreSQL, and general framework experimentation. NOT published as a library.

## What It Solves

- Full working web server example demonstrating multi-API pattern, parameter extraction, user history, auth, file download
- API interface examples exercising all annotation combinations
- Dynamic start/stop of PostgreSQL + LocalStack Testcontainers from within a running web server
- DynamoDB-backed user action logging with configurable TTL

## Key Details

- Depends on ~25 JSK modules
- Contains commented-out plugin configs preserved as usage examples
- Primarily serves as integration test and usage reference — not a production module
