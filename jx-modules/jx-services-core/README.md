# JX Services Core

Defines all core service interfaces and provides foundational abstractions for cluster workers, actor system, comparison tools, and typed ID wrappers. The service contract layer of JSK.

## What It Solves

- 19+ service interfaces: `ICoreServices`, `IAsync`, `IBytes`, `IJson`, `IHttp`, `ITime`, `IIds`, `IRand`, `ILog`, `IKvStore`, `IRepeat`, `IConfig`, `IMapper`, `IBoot`, `ITranslate`, `IRateLimiter`, `IExcept`, `IFree`, `IResCache`
- The standard Gson implementation reads numbers requested as `Object` or `Number` (including map/list members) as `BigDecimal` through the existing `from`/`fromWithNulls` methods. This preserves large integers and precise decimals. Explicit `Long`, `Double`, and other concrete numeric types keep their declared types. Code reading untyped numbers should use `Number` or `BigDecimal` rather than casting to `Double`.
- Cluster worker framework (26 files): `CluOnOffWorker`, `CluOnOffWithLockWorker`, `CluKvBasedOnOffWorker`, `CluScheduler`, split-task workers for parallel distributed processing
- Typed ID wrappers: `IdBase`, `IdString`, `IdLong`, `IdUuid` — compile-time type safety for identifiers

## Key Details

- 164 Java files — the second-largest module
- The cluster worker system is substantial (26 files, multiple patterns including KV-backed locking and scheduling)
- Depends only on `jx-utils`
