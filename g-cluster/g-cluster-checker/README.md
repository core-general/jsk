# G-Cluster Checker

Multi-threaded HTTP endpoint stress-tester that generates time-series PNG charts of response codes, exceptions, timeouts, and response times.

## What It Solves

- Spawns N worker threads, each repeatedly hitting a target URL for a configurable duration
- Collects response codes, timings, exceptions, and a custom `_nver` header (deployment version)
- Generates PNG charts via JFreeChart showing HTTP codes, exceptions, timeouts, response times, and version distribution over time

## Key Details

- Tracks `_nver` header — designed to verify deployment version transitions during rolling updates
- Depends on `maths/jfree` for chart generation
- Has an interactive REPL mode (`GccInteractiveMain`)
