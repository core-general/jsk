# Web Server Spring JPA Core

JPA-aware web server extension that converts optimistic locking exceptions to HTTP 503 with `must_retry` substatus.

## What It Solves

- `WebServerCoreWithJpaRetryHandling<T>` catches `ObjectOptimisticLockingFailureException`, `OptimisticLockException`, etc. and returns 503 + `must_retry` substatus instead of 500

## Key Details

- Single-class module
- Complements `RdbTransactionManagerImpl`'s retry at the DB layer with retry semantics at the web layer
- Client code should check for 503 + `must_retry` and retry the request
