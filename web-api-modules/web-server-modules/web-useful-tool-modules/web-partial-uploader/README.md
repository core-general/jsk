# Web Partial Uploader

Resumable/chunked file upload API — allows clients to upload large files in parts with metadata tracking, size validation, and completion callbacks.

## What It Solves

- `PupPublicApi<META, FINISH>` — API for creating uploads, uploading parts, and querying upload status
- `PupApiImpl<META, FINISH>` — abstract implementation using `IKvUnlimitedStore` for metadata and `PupIByteStorage` for raw bytes
- Validates part count, size limits, and upload state (not already finished/failed)
- Generic `META` and `FINISH` type parameters allow custom metadata and completion result types

## Key Details

- Designed as a reusable library — extend `PupApiImpl` and provide storage/user backends
- Uses `ISizedSemaphore` for upload concurrency limiting
