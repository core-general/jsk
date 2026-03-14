# Web Server Melody Patch

JavaMelody monitoring collector integration — manages monitored nodes (add/remove/health-check) in multi-node setups.

## What It Solves

- `MelServlet` — REST endpoints for node management: add/remove/list nodes
- `MelNodeManagementService` — manipulates JavaMelody's `Parameters.getCollectorUrlsByApplications()` directly
- `MelCleanTask` — background thread that periodically pings nodes and removes unreachable ones

## Key Details

- Uses `javax.servlet` (old API), not `jakarta.servlet`
- Directly patches JavaMelody internals — hence "melody-patch"
