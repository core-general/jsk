# Web Server Spring Core

Spring property binding for web server parameters — node info, secret management, and server configuration beans.

## What It Solves

- `WebServerNodeInfo` generates unique node ID (persisted via `INodeRestartStorage`), loads build info, aggregates diagnostics. Node ID format: `{timestamp}-{tinyHaiku}`
- `WebSecretSpringProvider` reads `spark_secret` and `spark_secret_list` from Spring properties
- `WebServerSpringParams` binds Spring properties for port, form/multipart limits, CORS, DDoS params, idle timeout

## Key Details

- Property names use `spark_` prefix (historical)
- Secret list uses `#` as separator (not comma)
- Only 3 files — thin wiring
