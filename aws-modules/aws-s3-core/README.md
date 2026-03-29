# AWS S3 Core

Full-featured S3 client with high-level operations including streaming, public URLs, and cross-bucket comparison.

## What It Solves

- `S3JskClient` provides put/get/delete objects, list buckets, streaming upload/download, public URL generation, and metadata retrieval
- `S3CompareTool` compares two S3 locations (possibly different accounts/regions) by listing objects and computing per-file SHA-256 hashes
- Supports force-path-style addressing for S3-compatible storage (MinIO, etc.)

## Key Details

- `S3CompareTool` uses parallel streams for performance
- Supports both AWS S3 and S3-compatible storage via URI-based addressing with `forcePathStyle()`
