# Web Server Core

The core server engine — reads API interface metadata, creates HTTP routes, builds a priority-sorted filter chain, and invokes implementation methods reflectively.

## What It Solves

- `WebServerCore<T>` creates routes from API interface + implementation, processes requests through a priority-sorted filter chain
- `WebServerCoreWithPings<T>` adds auto-generated auxiliary endpoints: `{basePath}/ping`, `/jskinfo`, `/api-info`, `/api-info-postman`, `/api-info-swagger`
- 9 standard filters (priority-ordered): context parsing → request logging → default headers → DDoS rate limiting → idempotence → exception handling → shutdown → auth → rendering

## Key Details

- Filter execution order is determined by priority values (TreeSet), NOT list/declaration order
- DDoS filter uses a "court/jail" model for rate limiting
- `WebServerContext` is an abstraction — actual HTTP server (Jetty) provides the implementation
