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

## HTTP response streaming

`IHttp.HttpBuilder.goStream` runs a scoped callback with `code()`, `headers()`, and `body()` for the response, including error statuses. It makes one attempt regardless of `tryCount`. The body is closed when the callback returns or fails; it must not escape the callback. The returned `CoreHttpResponse` contains metadata and an empty byte body.

```java
var result = http.get(url)
        .followRedirects(false)
        .connectTimeout(Duration.ofSeconds(2))
        .timeout(Duration.ofSeconds(10))
        .totalTimeout(Duration.ofSeconds(10))
        .goStream(response -> response.body().transferTo(output));
```

`totalTimeout` remains active while the callback consumes the body. Expiry closes the upstream stream and returns a timeout failure; interruption preserves the thread's interrupted flag. Callbacks should return promptly when their stream closes. The destination stream belongs to the caller.

Streaming delivers raw response bytes and does not automatically request gzip. `maxResponseBytes` can bound streamed bytes or buffered responses; buffered gzip responses are bounded before and after decompression. Existing buffered and file terminal operations retain their behavior, and `goToFile` uses its own byte limit. Builders are mutable and must not be shared between concurrent requests.
