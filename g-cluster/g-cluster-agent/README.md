# G-Cluster Agent

Lightweight agent process that runs on cluster nodes, polls S3 for new file versions (configs, JAR payloads), downloads them, and restarts local services for zero-downtime rolling deployments.

## What It Solves

- Periodically checks S3 for newer versions of tracked files, downloads updates, manages version state (good/bad/new)
- Rolling update protocol: acquire S3 lock → download → restart service → health-check → mark good or rollback
- Exposes health-check endpoint on port 8079 via SparkJava (`/agent/ping`)
- Automatic rollback to last known good version if health check fails

## Key Details

- Uses S3 as BOTH artifact storage AND distributed lock mechanism (optimistic locking with delay + re-check)
- Version state tracked locally via file-based storage
- Builds as a fat JAR
