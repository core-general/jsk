# JSK Utility Modules — Reference

> **JSK** — modular Java library: utilities, services with DI, backend building blocks. Covers `jx-modules/*` and `text-modules`.

## 1. jx-utils (`sk.utils`)

Zero external deps (only Lombok). Foundation for all JSK modules.

### 1.1 Static Utility Classes (`sk.utils.statics`)

Final classes, static methods only. 2-letter names by design.

#### `Cc` — Collections

| Category | Methods | Notes |
|---|---|---|
| **Create** | `l(items...)` `s(items...)` `m(k,v,...)` `tm(k,v,...)` `lhm(k,v,...)` `em(cls,k,v,...)` `q(items...)` | Mutable. `l`→ArrayList, `s`→HashSet, `m`→HashMap, `tm`→TreeMap, `lhm`→LinkedHashMap, `em`→EnumMap, `q`→ArrayDeque. Map factories check dup keys. |
| **Empty** | `lEmpty()` `mEmpty()` `sEmpty()` | Immutable (delegates `Collections.empty*()`) |
| **Join** | `join(delim, iter, toStr)` `joinMap(iDel, keyDel, map, toStrK, toStrV)` | `joinMap` stringers are `F2S<K,V>` — receive both key AND value |
| **Streams** | `stream(iterable)` `stream(iterator)` `addStream(s1,s2)` | Any Iterable/Iterator → Stream |
| **Collectors** | `toL()` `toS()` `toM(keyMapper)` `toM(keyMapper,valMapper)` `toMX2()` `toSortedM(...)` `toSortedMX2()` `toMEntry()` `toSortedMEntry()` | `toMX2()` collects X2 tuples→map, `toMEntry()` collects Map.Entry streams |
| **Iterate** | `eachWithIndex(iter,c)` `eachWithEach(t1,t2,c)` `eachSync(t1,t2,c)` `mapEachWithIndex(iter,f)` | Indexed/sync/cartesian |
| **List ops** | `first(l)` `last(l)` `getAt(l,i)` `sort(l,comp)` `reverse(l)` `shuffle(l,rnd)` `filter(l,pred)` `map(l,fn)` `fill(n,val)` | Return `O<T>` for safe access |
| **Map ops** | `compute(map,key,remap,start)` `putAll(map,maps...)` `groupBy(col,fn)` `mapBy(col,fn)` `filter(map,pred)` | Return the map (chaining) |
| **Set ops** | `addAll(col,cols...)` `removeAll(c1,c2)` `retainAll(c1,c2)` | Mutate-and-return |
| **Ordering** | `ordering(list)`→`Map<T,Integer>` `orderingComparator(ordering)` | Build comparator from reference list |
| **Split** | `splitIntoGroups(col, size)` | Split collection into sized groups |

`Cc.l("Alice","Bob")` → ArrayList, `Cc.m("Alice",95,"Bob",87)` → HashMap, `Cc.join(", ", items, Item::getName)`

#### `St` — Strings

| Category | Methods |
|---|---|
| **Check** | `isNullOrEmpty(s)` `isNotNullOrEmpty(s)` |
| **Transform** | `capFirst(s)` `lowFirst(s)` `repeat(s,n)` `count(s,sub)` |
| **Truncate** | `raze(s,limit)` `raze3dots(s,limit)` `splitBySize(s,max)` |
| **Filter** | `leaveOnlyLetters(s)` `leaveOnlyLettersAndNumbers(s,spaces)` |
| **Case** | `snakeToCamelCase(s)` `camelToSnake(s)` `camelToCapitalizedWords(s)` |
| **Regex** | `contains(s,pat)` `matchFirst(s,pat)` `matchAllFirst(s,pat)` `matchAll(s,pat)` |
| **Substr** | `sub(s,after,before)` `subLF/subLL/subRF/subRL(s,delim)` — L/R=left/right, F/L=first/last occurrence |
| **Encode** | `utf8(s)` `bytesToHex(b)` `hexToBytes(h)` |
| **Numbers** | `shortNumberForm(n)` — e.g. 1500→"1.5k", supports custom labels (Kb/Mb) |
| **Distance** | `levenshteinDistance(s1,s2)` `longestCommonSubstring(x,y)` |
| **Prefix/Suffix** | `startWith(s,p)` `endWith(s,sf)` `notStartWith(s,p)` `notEndWith(s,sf)` |
| **Format** | `addTabsLeft(text,n)` `minSymbolsOtherwisePrefix(num,min,prefix)` |

Constants: `St.eng` `St.ENG` `St.dig` `St.engDig` `St.ENGDig` `St.engENGDig` `St.UTF8`

#### `Ex` — Exceptions

| Method | Description |
|---|---|
| `toRuntime(F0E<T>)` / `toRuntime(RE)` | Wrap checked→RuntimeException |
| `thRow(Exception)` / `thRow(String)` | Throw as runtime; generic return allows use in expressions |
| `get(Callable<T>)` | Same as toRuntime for Callable |
| `getIgnore(Callable<T>)` | Returns null on exception |
| `runIgnore(RE)` | Ignores any exception |
| `traceAsString(e)` / `getInfo(e)` | Exception→string with stack trace |
| `isInstanceOf(e, classes...)` | Check type (unwraps UndeclaredThrowableException) |
| `throwIf(bool, desc)` | Conditional throw |
| `notImplemented()` | Throws NotImplementedException |

#### `Fu` — Functions

| Method | Description |
|---|---|
| `emptyR()` / `emptyC()` / `emptyCE()` | No-op runnable/consumer |
| `equal(a,b)` / `notEqual(a,b)` | Null-safe equals |
| `compare(a,b)` / `compareN(a,b)` | Null-safe Comparable comparison (N=nulls first) |
| `coalesce(a,b)` / `coalesce(a,b,c)` | First non-null→O |
| `bothPresent(oA,oB)` | O<X2<A,B>> if both present |
| `isTrue(Boolean)` | Null-safe boolean check |
| `run4ever(RE, onThrowable, shouldFinish)` | Infinite-loop runnable with error handling |
| `run(times, runnable)` | Execute N times |
| `notNull()` | Predicate for Objects::nonNull |

#### `Ti` — Times

Constants (ms): `Ti.second` `Ti.minute` `Ti.hour` `Ti.day` `Ti.week`
Formatters: `Ti.yyyyMMddHHmmss` `Ti.yyyyMMdd` `Ti.HHmmss` `Ti.yyyyMMddHHmmssSSS` `Ti.yyyyMMddHHmmss_ONLY_DIGITS`
Zones: `Ti.Moscow` `Ti.UTC`

| Method | Description |
|---|---|
| `between(start, end)` | Duration — works with LocalDate/DateTime, ZonedDateTime, Instant, long |
| `isSequence(dates...)` | Check chronological order |
| `naiveProfile(r, count)` / `naiveProfileMS(r, count)` | Simple benchmarking |
| `cronEveryXSeconds/Minutes(n)` / `cronEveryDayByHourMinute(h,m)` | Cron expression generators |
| `sleep(ms)` | Thread sleep |

#### `Ma` — Maths

| Method | Description |
|---|---|
| `median(longs)` / `medianD(doubles)` | Median |
| `clamp(val, min, max)` | For long/double (handles NaN/Infinity) |
| `inside(val, min, max)` | Range check for int/double |
| `rand(min, max)` | Random double in range |
| `isInt(s)` / `isFloat(s)` | String→numeric validation |
| `pi(s)` `pl(s)` `pd(s)` `pf(s)` `pb(s)` | Parse int/long/double/float/boolean |
| `optimalSampleSize(accuracy, error, fullSize)` | Statistical sample size (SampleSizeAccuracy enum, double error%, O<Long> fullSize) |
| `mean(a,b)` | Average of two |

#### `Io` — Input/Output

| Category | Methods |
|---|---|
| **Read** | `sRead(path)`→LineReader (`.string()` `.oString()` `.lines()` `.oLines()` `.lineStream(c)` `.lineStreamMap(f)`), `bRead(path)`→ByteReader (`.bytes()` `.oBytes()` `.oIs()`) |
| **Write** | `reWrite(path, C1<PlainWriter>)` — overwrite, `addWrite(path, C1<PlainWriter>)` — append, `reWriteBin(path, C1<BinaryWriter>)`, `addWriteBin(path, C1<BinaryWriter>)` |
| **File ops** | `visitEachFile(path,c)` `fileToStructure(path)`→FileList `exists(path)` `deleteIfExists(path)` `copy(from,to)` `move(from,to)` `isWWWAvailable()` |
| **Streams** | `streamPump(is)`→byte[] `streamPump(in,out,bufSize,interceptor)` `bytesToStream(bytes)` |
| **Resources** | `getResource(name)`→O<String> `getResourceBytes(name)`→O<byte[]> `getResourceStream(name)`→O<InputStream> `isResourceExists(name)` |
| **Network** | `changePortForUrl(url,port)` `getFreePort(storage)` |
| **URI** | `getFileFromUri(uri)` `isJarUri(uri)` `getJarContextPathFromUri(uri)` |
| **Execute** | `execute(cmd)`→ExecuteInfo `executeAndFail(cmd)` `serviceStart/Stop/Restart/Status(svc)` |
| **Serialize** | `serialize(data)`→byte[] `deSerialize(bytes,cls)`→T |

Write uses callback: `Io.reWrite("f.txt", w -> w.appendLine("Hello"));`

#### `Ar` — Arrays

For `double[]`, `byte[]`, `int[]`: `mapAll(summarizer,forceSameSize,arrays...)` `map(arr,op)` `fill(size,val)` `avg(arr)` `sortCopy(arr)` `intToByteArray(int)` `longToByteArray(long)` `copy(bytes,start,len)`

#### `Im` — Images

`readImage(file/bytes/stream)` `writeImage(img,fmt,quality,out)` `resize(img,w,h)` `blur(img,radius)` `toBytes(img,fmt)`

#### `Re` — Reflections

| Method | Description |
|---|---|
| `createObjectByDefault(cls)` | Instantiate via default constructor→O<T> |
| `findInEnum(cls,name)` / `findInEnumIgnoreCase(cls,text)` | Safe enum lookup→O<T> |
| `findInEnumById(text,cls)` | Find enum by Identifiable.getId() |
| `getClassIfExist(className)` | Class lookup without exception→O<Class<?>> |
| `getParentParameters(cls)` / `getFirstParentParameter(cls)` | Generic superclass type params |
| `getAllNonStaticFields(cls)` / `getNonStaticPublicFields(cls)` | Field enumeration→SortedSet<Field> |
| `accessor(field)` / `getter(field)` / `setter(field)` | Reflective field access |
| `singleProxy(cls,handler)` / `doubleProxy(cls,cls2,handler)` | Dynamic proxy creation |

### 1.2 Functional Types (`sk.utils.functional`)

JSK functional interfaces **declare checked exceptions** on every lambda, eliminating try-catch inside lambdas.

| Type | Signature | Notes |
|---|---|---|
| `F0<T>` / `F0E<T>` | `()→T` | Supplier (E=with checked exception) |
| `F1<A,B>` / `F1E<A,B>` | `A→B` | One-arg function |
| `F1S<A>` | `A→String` | Shorthand F1<A,String> |
| `F2<A,B,C>` / `F2S<A,B>` | `(A,B)→C` | Two-arg (F2S returns String, used in Cc.joinMap) |
| `F3<A,B,C,D>` | `(A,B,C)→D` | Three-arg |
| `C1<A>` / `C1E<A>` | `A→void` | Consumer (E=checked) |
| `C1Char` / `C1Int` | `prim→void` | Primitive consumers |
| `C2<A,B>` / `C3<A,B,C>` | multi→void | Bi/tri-consumer |
| `P1<A>` / `P2<A,B>` | `→boolean` | Predicate/bi-predicate |
| `R` / `RE` | `()→void` | Runnable (RE=checked) |
| `Op1<T>` / `Op2<T>` | `T→T` / `(T,T)→T` | Unary/binary operator |
| `Gett<T>` / `Sett<T>` / `GSet<T>` | get/set | Getter, Setter, combined. `GSetImpl<T>` default impl. `GSetO<T>`/`GSetOImpl<T>` Optional variants |
| `Converter<A,B>` | `convertThere(A)→B` `convertBack(B)→A` | Bidirectional. `ConverterImpl<A,B>` default impl |

#### `O<T>` — Short Optional

**Serializable replacement for `java.util.Optional`** used everywhere in JSK.

- Factory: `O.of(val)` `O.ofNull(val)` `O.ofNullable(val)` `O.empty()` (`ofNull`≡`ofNullable`)
- Interop: `O.of(Optional)` / `o.toOpt()`
- Chain: `o.or(F0<O<T>>)` `o.orVal(F0<T>)` `o.ifPresentOrElse(consumer, emptyAction)`
- Combine: `O.allNotNull(oA, oB, mapper)` (also 3-arg with F3)
- Fold: `o.collect(mapper, ifNone)` — apply mapper if present, ifNone if empty
- Other: `o.stream()` `o.size()` (0 or 1)

#### `OneOf<L, R>` — Either Type

Left-or-right disjoint union. **Convention: left=success, right=error**. Both null→IllegalArgumentException.

Factory: `OneOf.left(val)` `OneOf.right(err)`. Transform: `map(lFn,rFn)` `mapLeft` `mapRight` `flatMap`. Access: `oLeft()` `oRight()` `left()` `right()` `leftOrThrow()`. Fold: `collect(lFn,rFn)` `collectSelf()` `collectRight(rFn)` `collectLeft(lFn)`. Exception capture (RuntimeException only): `OneOf.checkException(()->call())` or `checkException(()->call(), MyEx.class)`

#### Pair/Union Types

| Type | Constraint |
|---|---|
| `Both<L,R>` | Both must be non-null |
| `BothOrNone<L,R>` | Both present or both absent |
| `OneOrBoth<L,R>` | At least one non-null |
| `OneOrNone<L,R>` | Exactly one or neither (like OneOf + both-null) |
| `OneBothOrNone<L,R>` | No constraint |

All have `left()` `right()` `oLeft()` `oRight()` `map(...)` `collect(...)` and factory methods.

#### `Lazy<T>` / `LazyGS<T>`

Lazy init wrapper. **Not thread-safe**. `lazy.get()` (compute on first call), `lazy.isSet()`, `lazy.refresh()`. `LazyGS<T>` adds setter support.

### 1.3 Tuples (`sk.utils.tuples`)

`X.x(a)` through `X.x(a,b,c,d,e,f,g)` → `X1` through `X7`. Fields: `i1` `i2` ... (public + accessor methods). Implements `AsList`, proper `equals`/`hashCode`.

### 1.4 Async Primitives (`sk.utils.async`)

| Class | Description |
|---|---|
| `ForeverThreadWithFinish` | Infinite loop thread with graceful shutdown via `finishThread()`→CompletableFuture<Boolean>. Takes `C1<CancelGSetter>` or `R`. |
| `CancelGSetter`/`CancelGetter`/`CancelSetter` | Cooperative cancellation: check `isCancelled()`, call `setCancelled(bool)` |
| `JLock` / `JReadWriteLock` | ReentrantLock/RWLock wrappers with lambda API |
| `AtomicNotifier` / `SimpleNotifier` | Wait/notify with cleaner API |
| `GuaranteedOneTimeTask` | Task runs exactly once across concurrent callers |

### 1.5 Collections & Data Structures (`sk.utils.collections`)

| Class | Description |
|---|---|
| `DequeWithLimit` | Max-size deque, oldest evicted |
| `MultiBiMap` | Bidirectional multi-map |
| `OneItemCollection` | Holds exactly one item |
| `UpdateableCacheWithMustValueOnStart` / `...OptionalValueOnStart` | Self-updating caches with configurable refresh |
| `Batch` | Batch processing helper |
| `ByteArrKey` | byte[] wrapper with proper equals/hashCode for map keys |
| **Cluster Sorter** (`cluster_sorter/`) | Merge sorted streams from multiple sources — forward/backward, batch/simple |
| **Priority Task Queue** (`task_queue/`) | Priority-based task queue with pluggable priority types |

### 1.6 Events & Pub/Sub (`sk.utils.events`)

`SimpleEvent` (observable), `ObjectEvent<T>` (typed payload), `JskPublisher<T>`/`JskSubscriber<T>` (typed pub/sub), `JskPubSubNoState` (stateless), `JskPubSubQueue` (queue-backed)

### 1.7 Files / CSV (`sk.utils.files`)

`CsvReader` `CsvWriter` `FileList` (Map<String,byte[]> = file tree) `PathWithBase` (path with base dir for relative resolution)

### 1.8 MinMax & Math (`sk.utils.minmax`, `sk.utils.math`)

`MinMax`/`MinMaxL` (double/long), `MinMaxAvg` (+running avg), `MinMaxMany`/`MinMaxManyL` (top-N), `MinMaxWithObject<T>`/`MinMaxAvgWithObj<T>` (+associated obj), `KahanSum` (stable summation), `LDouble` (Comparable double wrapper)

### 1.9 Other Utilities

| Package | Classes | Description |
|---|---|---|
| `sk.utils.semver` | `Semver200` `Semver200Template` `MultiSemver` | Semantic versioning with wildcards |
| `sk.utils.tree` | `Tree<K,V>` `TreeNode` `TreePath` | Generic tree with traversal |
| `sk.utils.paging` | `SimplePage` `CountablePage` `RingPicker` | Pagination helpers |
| `sk.utils.random` | `MapRandom` | Weighted random from probability map |
| `sk.utils.javafixes` | `TypeWrap` `FieldAccessor` `ArgParser` `Base62` `CheckUtf8` `BadCharReplacer` `ImgFormat` | Type tokens, CLI parsing, encoding |
| `sk.utils.asserts` | `JskAssert` `JskAssertException` | Custom assertions |
| `sk.utils.computation.chained` | `ChainedComputation` `ChainedMappedInput` | Reactive/chained computation graph |
| `sk.utils.ifaces` | `Identifiable<T>` `IdentifiableString` `AsList` | Marker interfaces |
| `sk.utils.logging` | `JskLogging` | Logging system setup interface |
| `sk.utils.land` | `JskLandLoadedToken` `JskWithChangedPort` | App bootstrap tokens |
| `sk.exceptions` | `JskProblem` `JskProblemException` `NotImplementedException` `Problem` | Structured problems (code, substatus, description). JskProblemException wraps JskProblem. |

---

## 2. jx-services-core (`sk.services`) — Interfaces Only

`ICoreServices` facade: `async()` `bytes()` `http()` `ids()` `json()` `iLog()` `rand()` `resCache()` `repeat()` `times()` `free()` `except()` `sizedSemaphore()`

### Service Interfaces

| Interface | Key Responsibilities |
|---|---|
| **`IAsync`** (`sk.services.async`) | Thread pools: `fixedExec` `bufExec` `singleExec` `scheduledExec`, `coldTaskFJP` (Fork/Join), `runAsyncDontWait` `supplyAsyncDontWait`, named threads. Extends `ISleep`. |
| **`IBytes`** (`sk.services.bytes`) | `md5` `sha256` `crc32`/`crc32c` `crcAny64/128/256`, Base64/Base62, URL encode/decode, BCrypt hash/check, AES encrypt/decrypt, gzip/ungzip, zip archive/unarchive (with password), data compression |
| **`IJson`** (`sk.services.json`) | `to(obj)` `from(json,cls)` `toPretty(obj)` `validate(json)` `beautify(json)` `jsonPath(json,path,type)`, AES-encrypted JSON, polymorphic deser |
| **`IHttp`** (`sk.services.http`) | HTTP client with builder pattern (see below) |
| **`ITime`** (`sk.services.time`) | `now()` `nowZ()` `nowI()` `nowLD()` `nowLT()` `toZDT(ms)` `toLD(ms)` `getDifWith(old)`. Configurable ZoneId/Clock. |
| **`IIds`** (`sk.services.ids`) | `shortId()`→UUID, `customId(len)`, `text2Uuid(val)`, `unique(val)` (deterministic hash), `genUniquePngImage(id)` (identicons), haiku names (`longHaiku` `shortHaiku` `tinyHaiku` `timedHaiku`) |
| **`IRand`** (`sk.services.rand`) | `rndBool(prob)` `rndInt(bound)` `rndDouble(min,max)` `rndFromList(list)` `rndDist(weights)` (weighted), `rndString(from,to,charset)` |
| **`ILog`** (`sk.services.log`) | `log(cat,type,info)` `logExc(e)` `agg(...)` (aggregated) `both(...)` (log+agg). Levels: TRACE, DEBUG, INFO, ERROR. |
| **`IKvStore`** (`sk.services.kv`) | KV store with optimistic locking: `getAsString/Object/Bool/Long/Int`, `updateString/Object/Bool/Long`, `trySaveNew*`, `tryLockOrRenew`, `clearValue`, `clearAll`. Versioned items. |
| **`IRepeat`** (`sk.services.retry`) | Retry logic with batch support |
| **`IResCache`** (`sk.services.rescache`) | Resource caching |
| **`IFree`** (`sk.services.free`) | Free-form utility service |
| **`IExcept`** (`sk.services.except`) | Exception handling service |
| **`IConfig`** (`sk.services.appconf`) | App config with typed config units |
| **`IMapper`** (`sk.services.mapping`) | `clone(obj,deep)` `map(in,outClass,deep)` `map(in,out,deep,copyNulls)` |
| **`IAppProfile`** (`sk.services.profile`) | App profile management (dev/test/prod) |
| **`IBoot`** (`sk.services.boot`) | App bootstrap: `run()` |
| **`ITranslate`** (`sk.services.translate`) | Text translation |
| **`IRateLimiter`** (`sk.services.ratelimits`) | Rate limiting |

#### `IHttp` — Builder Pattern

| Method | Builder | Description |
|---|---|---|
| `get(url)` | `HttpGetBuilder` | GET |
| `head(url)` | `HttpHeadBuilder` | HEAD |
| `postBody(url)` | `HttpBodyBuilder` | POST raw body |
| `postForm(url)` | `HttpFormBuilder` | POST form params |
| `postMulti(url)` | `HttpMultipartBuilder` | POST multipart |
| `deleteBody/Form/Multi(url)` | same types | DELETE variants |

Shared: `.tryCount(n)` `.trySleepMs(ms)` `.timeout(Duration)` `.login(s)` `.password(s)` `.headers()`→Map
Terminal: `.go()`→`OneOf<String,Exception>` `.goBytes()` `.goResponse()` `.goAndThrow()`
Shortcuts: `getS(url)` (GET→String), `getB(url)` (GET→bytes)

Usage: `http.get(url).tryCount(5).timeout(Duration.ofSeconds(30)).go().left()`

### Additional Core Components

| Component | Description |
|---|---|
| **Cluster Workers** (`sk.services.clusterworkers`) | Distributed task processing: `CluOnOffWorker` `CluOnOffWithLockWorker` `CluScheduler` `CluKvBasedOnOffWorker` `CluKvSplitTaskWorker`. Uses IKvStore. |
| **Comparer** (`sk.services.comparer`) | Collection/map/set comparison→`CompareResult` (added/removed/changed) |
| **Actor System** (`sk.services.actors`) | Lightweight: `ActorSystem` `PublicActor` `PrivateActor` |
| **Id Types** (`sk.utils.ids`) | Typed wrappers: `IdBase<T>` `IdString` `IdInt` `IdLong` `IdUuid` |

---

## 3. jx-services-standard-impl

Default implementations. `CoreServicesRaw.services()` → fully wired DI-free instance.

| Impl → Interface | Notes |
|---|---|
| `CoreServicesRaw`→`ICoreServices` | Factory for all services |
| `AsyncImpl`→`IAsync` | ExecutorService/ForkJoinPool |
| `BytesImpl`→`IBytes` | JDK-based |
| `JGsonImpl`→`IJson` | Gson + polymorphism |
| `UtcSettableTimeUtilImpl`→`ITime` | UTC settable |
| `IdsImpl`→`IIds` | UUID+hashing (SecureRandImpl) |
| `RandTestImpl`→`IRand` | Test-oriented; `SecureRandImpl` also available |
| `HttpImpl`→`IHttp` | java.net.http.HttpClient |
| `ILogConsoleImpl`→`ILog` | Console |
| `RepeatImpl`/`ResCacheImpl` | Standard |
| `Freemarker`→`IFree` | Freemarker |

---

## 4. jx-model-mapper

`ModelMapperImpl` (wraps ModelMapper). Methods: `clone(obj,deep)` `map(in,outClass,deep)` `map(in,out,deep,copyNulls)`. Built-in converters: Long↔ZonedDateTime, Long↔Instant, ZDT↔Instant, Enum↔String, Optional↔Optional, O↔O. Extensible via `List<ModelMapperConfig>`. Uses `ITime` for tz-aware conversions.

---

## 5. jx-logging

`JskLoggingLogback` (sub-module `jx-logging-logback`): configures Logback via `logback{suffix}.xml` resources. Sets `logback.configurationFile` system property. Validates no conflicting default exists.

---

## 6. jx-test-modules

| Module | Base Class | Description |
|---|---|---|
| `jx-test` | `JskMockitoTest` | Auto opens/closes Mockito mocks via @BeforeEach/@AfterEach |
| `jx-test-with-services` | `MockitoTestWithServices` | Extends above. All core services as `@Spy` (async, bytes, http, rand, ids, times, json, iLog, resCache, repeat, free, sizedSemaphore, except). Uses `CoreServicesRaw.services()`. |
| `jx-test-spring` | `JskSpringTest<T>` | Spring integration tests. Implement `getRootConfig()` + `getProfile()`. |
| `jx-test-landscape-core` | `JskLandScape` `JskFullLand` `JskLand` `JskLandScapeParallel` | Integration/landscape testing with state mgmt mixins |
| `jx-test-landscape-testcontainers` | | Testcontainers base |
| `jx-test-landscape-testcontainers-pg` | | PostgreSQL testcontainer |
| `jx-test-landscape-testcontainers-localstack` | | LocalStack (AWS) testcontainer |

---

## 7. jx-system-info

`JxSystemInfoService` (OSHI): `getProcessorLoad(delay)`→per-core+avg CPU, `getGeneralMemoryInfo()`→system RAM, `getProcessMemoryUsed()`→JVM heap/non-heap. Has `main()` with interactive load testing.

---

## 8. jx-ip-geo-maxmind

`IpGeoMaxmindExtractor` (implements `IIpGeoExtractor`): loads MaxMind GeoLite2 `.mmdb` (URL or classpath), `ipToGeoData(ip)`→`O<IpGeoData>` (country, timezone). `IpGeoCache` adds caching.

---

## 9. jx-probability-algs

`BloomFilterImpl` extends `ICountSetExistence`: Bloom filter via `com.sangupta.bloomfilter`. Config: `maxItems`, `probabilityOfFalsePositives`. Serializable to/from `byte[]`. Methods: `addElement` `isElementExist` `serialize` `deSerialize` — overloads for byte[], int, long, String, T.

---

## 10. text-modules

### utf-2-ascii

`Uni2AsciiDecoder` / `Uni2AsciiDecoderJunicodeImpl`: `decodeSimple(input)` (Unicode→ASCII), `decodeUrlLower(input)` (URL-safe slug with `-`). Built on junidecode with custom Cyrillic/Thai fixes.

---

## 11. Key Patterns

- **`O<T>`** everywhere instead of Optional — serializable, extra combinators
- **`OneOf<L,R>`** for error handling (left=success, right=error). Pair/Union family: `Both` `BothOrNone` `OneOrBoth` `OneOrNone` `OneBothOrNone`
- **Checked lambdas**: `F0E` `F1E` `RE` `C1E` declare `throws Exception`
- **Service pattern**: interface in `jx-services-core`, impl in `jx-services-standard-impl`. `CoreServicesRaw.services()` = DI-free. DI via Jakarta `@Inject` + Spring.
- **Fluent chaining**: Cc mutate-and-return: `Cc.sort(Cc.add(Cc.l("b","a"), "c"))`
- **Structured errors**: `JskProblem` (code, substatus, desc) + `JskProblemException`
