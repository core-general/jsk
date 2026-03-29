# G-Cluster Deployer

CLI tool to deploy files (JARs, configs) to S3 with versioned metadata, plus a deployment verification checker.

## What It Solves

- `GcdDeployerMain` uploads files to versioned S3 paths, updates meta files that the agent watches, maintains history of last 25 deployments
- `GcdDeployCheckerMain` polls multiple URLs checking `_nver` header to verify all nodes picked up the new version
- Supports configurable retry count, time limits, and consecutive success requirements

## Key Details

- Builds two separate fat JARs (`deploy` and `deployChecker` profiles)
- The checker disables DNS caching to catch failover scenarios
- Deployment meta uses `DequeWithLimit` for version history
