# JSK Web Framework Reference

Interface-first, annotation-driven web framework. Define API as a Java interface → same interface drives server routing, client proxy generation, and documentation.

## Key Type: `O<T>`

`O<T>` (from `sk.utils.functional.O`) = short alias for `Optional<T>`. Methods: `O.of(val)`, `O.empty()`, `O.ofNull(val)`. Both `O<T>` and `java.util.Optional<T>` supported as optional parameters in API interfaces.

## Maven Module Structure

```
web-api-modules/
├── web-core/                    # Annotations, types, interfaces (no Spring)
│   ├── sk.web.annotations.*     # @WebPath, @WebGET, @WebPOST, @WebAuth, @WebRender, etc.
│   ├── sk.web.infogatherer.*    # WebClassInfo, WebMethodInfo, WebClassInfoProvider
│   ├── sk.web.renders.*         # WebRenderType, WebFilterOutput, WebRender
│   ├── sk.web.auth.*            # WebAuthServer, WebAuthClient, WebSecretProvider
│   └── sk.web.utils.*           # WebUtils, WebApiMethod
├── web-core-spring/             # Spring config: WebCoreConfig (ApiClassUtil + WebMethodInfoProviderImpl)
├── web-server-modules/
│   ├── web-server-core/         # Server engine, filters, params, context abstractions
│   │   ├── sk.web.server.WebServerCore           # Route registration, filter chain, invocation
│   │   ├── sk.web.server.WebServerCoreWithPings  # Adds /ping, /jskinfo, /api-info, /api-info-postman, /api-info-swagger
│   │   ├── sk.web.server.filters.*               # Filter interfaces + implementations
│   │   ├── sk.web.server.params.*                # WebServerParams, WebDdosParams, etc.
│   │   └── sk.web.server.context.*               # Request/response abstractions
│   ├── web-server-spring-core/                    # Spring properties (WebServerSpringParams, WebSecretSpringProvider)
│   ├── web-server-spark-spring-melody/            # Jetty/Spark + JavaMelody implementation
│   │   ├── sk.web.server.spark.WebJettyServerStarter   # Starts embedded Jetty
│   │   ├── sk.web.server.spark.WebJettyEntryPoint      # SpringAppEntryPoint for Jetty
│   │   └── sk.web.server.spark.spring.WebSparkCoreConfig # Full Spring wiring
│   ├── web-server-spring-jpa-core/    # Spring Boot + JPA integration
│   ├── web-server-melody-patch/       # JavaMelody patches
│   ├── web-useful-tool-modules/       # Utilities (e.g. partial uploader)
│   └── web-vaadin-common/             # Vaadin integration
├── web-client-modules/
│   ├── web-client-core/         # Client factory, result handlers, platform abstraction
│   │   ├── sk.web.client.WebClientFactory         # Creates dynamic proxy clients
│   │   ├── sk.web.client.WebClientResultHandler   # Processes HTTP responses
│   │   ├── sk.web.client.WebClientInputHandler    # Pre-request hook
│   │   └── sk.web.client.WebPlatformSpecificHelper # Platform abstraction
│   ├── web-client-core-java/    # Java platform helper (java.lang.reflect.Proxy)
│   ├── web-client-core-teavm/   # TeaVM platform helper (compile-time generated)
│   ├── web-client-core-spring/  # Spring config (WebClientConfig)
│   └── web-client-swagger/
│       └── web-client-swagger-generator/  # OpenAPI/Swagger spec generation
mvn-plugins/
├── web-api-info-generator-maven-plugin/   # REQUIRED: Parses API source → metadata JSON
└── web-client-swagger-maven-plugin/       # Generates Swagger + optional Dart/TS clients
```

## Defining Web API Endpoints

### Annotations

| Annotation | Target | Purpose |
|---|---|---|
| `@WebPath("path")` | Type/Method | URL path. `appendMethodName` (default `true`) controls method name appending. |
| `@WebGET` | Method | HTTP GET endpoint |
| `@WebPOST` | Method | HTTP POST (multipart by default). `forceMultipart=true` to require it. |
| `@WebMethod(method=...)` | Method | Fine-grained: `POST_MULTI`, `POST_MULTI_SURE`, `POST_FORM`, `POST_BODY`, `GET` |
| `@WebAuth` | Type/Method | Secret-based auth. Attrs: `paramName` (default `"_secret"`), `isParamOrHeader` (default `true`=param), `getPassword`, `srvProvider`, `clientProvider`. |
| `@WebAuthNO` | Method | Disables auth (overrides class-level `@WebAuth`) |
| `@WebAuthBasic` | Type/Method | HTTP Basic Auth. `realmName`, `forceParametersExist`. |
| `@WebIdempotence` | Type/Method | Enables idempotency. `paramName`, `force`, `retryCount`, `retrySleepMs`. |
| `@WebIdempotenceNO` | Method | Disables idempotency (overrides class-level) |
| `@WebRender(WebRenderType.X)` | Type/Method | Output format: `JSON` (default), `JSON_PRETTY`, `RAW_STRING`, `BASE64_BYTES`, `RAW_BYTE_ZIPPED` |
| `@WebUserToken` | Type/Method | Extracts user token from request param/header |
| `@WebRedirect` | Method | Redirect after execution. `redirectPath`, `addModelFieldsAsRedirectParameters`. |
| `@WebFile(filename=...)` | Method | File download endpoint |
| `@WebAllParams` | Parameter | Injects all request parameters as map |
| `@WebParamsToObject` | Parameter | Maps request params to object fields (supports `O<T>`/`Optional<T>` fields) |

### Example API Interface

```java
@WebPath("test")
public interface TestApi1 {
    @WebGET @WebPath(value = "ajk/:abc", appendMethodName = false)
    @WebUserToken(paramName = "abc") @WebRender(WebRenderType.RAW_STRING) @WebIdempotence
    String a(String abc);

    @WebGET @WebPath(value = "less", appendMethodName = false)
    @WebUserToken(paramName = "abc")
    SomeClass1 b(SomeClass2 abc, SomeEnum x);

    @WebPOST @WebAuth @WebIdempotence(force = true)
    Map<String, Integer> testWebUserToken(Map<String, String> a);

    @WebGET @WebRedirect(redirectPath = "https://google.com/")
    Map<String, Object> redirectTo();

    @WebGET @WebFile(filename = "test.file")
    byte[] bytes();

    @WebGET List<String> getStrings(List<String> abc);
    @WebGET String exceptTest();

    @WebGET @WebAuthBasic(realmName = "XXX", forceParametersExist = true)
    String basicAuthTest();

    @WebGET @WebRender(WebRenderType.RAW_STRING)
    String testParamsToObjectMergerGet(@WebParamsToObject SomeClass2 someCls2);

    @WebGET @WebRender(WebRenderType.RAW_STRING)
    String startEnvironment(O<Integer> port);
}
```

### URL Path Construction

Paths combine: **base path** + **class `@WebPath`** + **method `@WebPath`** + **method name** (unless `appendMethodName=false`).
Path parameters use `:paramName` syntax (e.g., `ajk/:abc`).

### Parameter Handling

| Type | Handling |
|---|---|
| Primitives, String, UUID, enum | Direct extraction from request params |
| `O<T>` / `Optional<T>` | Optional parameter (null allowed) |
| Complex objects | Deserialized from JSON string parameter |
| `byte[]` | Raw bytes from body or multipart part |
| `@WebParamsToObject` | Maps individual params to object fields |
| `@WebAllParams` | All params as map |

### WebMethodType Enum

`GET`, `POST_MULTI` (default for @WebPOST), `POST_MULTI_SURE` (must be multipart), `POST_FORM`, `POST_BODY` (single param only).

## Creating a Web Server

**1. Define API interface** (see above)

**2. Implement it:**
```java
public static class TestApiImpl implements TestApi1 {
    @Inject WebContextHolder ctx;
    @Inject IJson json;

    @Override public String a(String abc) { return abc; }
    @Override public SomeClass1 b(SomeClass2 abc, SomeEnum x) {
        return new SomeClass1(SomeEnum.THREE, "str", O.of(1), abc, new SomeClass3(), 5);
    }
    // ... implement all methods
}
```

**3. Register WebServerCore bean:**
```java
@Bean
public WebServerCore<TestApi1> WebServerCore(TestApiImpl impl, WebUserActionLoggingFilter actionLogger) {
    return new WebServerCoreWithPings<TestApi1>(TestApi1.class, impl) {
        @Override
        protected O<List<WebServerFilter>> getAdditionalFilters(O<Method> methodOrAll) {
            return of(Cc.l(actionLogger));
        }
    };
}
```

- `WebServerCore<API>` — reads interface annotations, creates routes, builds filter chain
- `WebServerCoreWithPings<API>` — adds auto-endpoints **prefixed with API base path**: `{basePath}/ping`, `/jskinfo`, `/api-info` (BasicAuth), `/api-info-postman` (BasicAuth), `/api-info-swagger` (BasicAuth)
- Multiple `WebServerCore` beans allowed for different API interfaces in one app

**4. Spring Configuration:**
```java
@Configuration
@Import({SpringCoreConfig.class, WebCoreConfig.class, Config.WebSparkCoreConfigThis.class, SpringCoreConfigWithProperties.class})
public static class Config {
    @Configuration
    public static class WebSparkCoreConfigThis extends WebSparkCoreConfig {}
}
```

`WebSparkCoreConfig` registers: `WebJettyServerStarter`, all filters, all renders, `WebContextHolder`, DDoS params, auth, JavaMelody, etc.

**5. Start:**
```java
public static void main(String[] args) {
    SpringApp.createWithWelcomeAndLogAndInit("Hello!", new JskLoggingLogback("tst_logger"),
            new WebJettyEntryPoint(), Config.class);
    Io.endlessReadFromKeyboard("exit", in -> {});
}
```

`WebJettyEntryPoint` injects `WebJettyServerStarter` and calls `server.run()`. Server takes `WebServerParams` + `List<WebJettyContextConsumer>` (e.g., `WebJettyContextConsumer4Spark` for routes, `WebJettyContextConsumer4Melody` for monitoring).

## WebContextHolder

Thread-local request context. Inject and use inside API implementations:
```java
@Inject WebContextHolder ctx;
// ctx.get().getParamAsString(name), getRequestHeader(name),
// setResponseHeader(name, value), getClientIdAndTokenCookie(cookieName)
```

## Creating a Java Client

Same API interface → type-safe HTTP client via `WebClientFactory`:

```java
// Full version (with pre-request hook)
O<API> createWebApiClient(String basePath, Class<API> apiCls,
    WebClientInputHandler inputHandler, WebClientResultHandler<E> resultHandler)

// Convenience (no input handler)
O<API> createWebApiClient(String basePath, Class<API> apiCls, WebClientResultHandler<E> resultHandler)
```

Creates a **dynamic proxy** that translates method calls → HTTP requests using annotation metadata. Handles path param substitution, `@WebAuth` secrets, `@WebIdempotence` keys automatically.

- `WebClientInputHandler` — pre-request hook to modify `WebApiClientExecutionModel` (headers, URL, etc.)
- `WebClientResultHandler<E>` — processes responses. Default: `WebClientResultHandlerSimpleJsonImpl` (JSON deserialization)
- Spring setup: import `WebClientConfig` (registers `WebAuthClientWithStaticSecrets` + `WebJavaSpecificHelper`)

### Multi-Platform Support

`WebPlatformSpecificHelper` interface with implementations:
- `web-client-core-java/` — `java.lang.reflect.Proxy`
- `web-client-core-teavm/` — compile-time generated classes

## Filter Chain

Filters sorted by `getFilterPriority()` in a `TreeSet`. Chain built via `descendingSet()` — **lowest priority = executes first**.

| # | Filter | Priority | Purpose |
|---|---|---|---|
| 1 | `WebContextExplicatorFilter` | -2,000,000 | Explicates request context |
| 2 | `WebRequestLoggingFilter` | 0 | Logs request info |
| 3 | `WebDefaultHeadersFilter` | 1,000,000 | Sets default response headers |
| 4 | `WebDdosFilter` | 2,000,000 | Rate limiting / DDoS protection |
| 5 | `WebIdempotenceFilter` | 3,000,000 | Idempotency handling |
| 6 | `WebExceptionFilter` | 4,000,000 | Catches and renders exceptions |
| 7 | `WebShutdownFilter` | 5,000,000 | Returns 503 during shutdown |
| 8 | `WebAuthFilter` | 6,000,000 | Validates `@WebAuth` secrets |
| 9 | `WebRenderFilter` | 7,000,000 | Prepares rendering context |
| | → **Method invocation** | | |

`PRIORITY_STEP = 1,000,000`. `WebUserActionLoggingFilter` (priority: -1,000,000) is an additional filter, not in default set.

### Filter Interface

```java
public interface WebServerFilter extends Comparable<WebServerFilter> {
    int getFilterPriority();
    <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext);
}
```

Each filter can: pre-process, call `requestContext.next().invokeNext()`, post-process, or short-circuit by returning `WebFilterOutput` without calling next.

### Adding Custom Filters

Override `getAdditionalFilters(O<Method> methodOrAll)` on `WebServerCore`. `methodOrAll`: `O.empty()` = all methods, `O.of(method)` = specific endpoint.

### Custom Exception Processors

Override `getExceptionProcessors(O<Method> methodOrAll)` on `WebServerCore` for per-method exception handling.

## Authentication

### Secret-based (`@WebAuth`)

- **Server**: `WebAuthServer.authenticate(O<String> secret)` validates. Default: `WebAuthServerWithStaticSecrets` using `WebSecretProvider.getPossibleSecrets()`.
- **Client**: `WebAuthClient.getSecret4Client()` provides secret. Default: `WebAuthClientWithStaticSecrets`.
- `isParamOrHeader`: `true` = request param, `false` = header
- `getPassword` non-empty → static password, bypasses providers

### Basic Auth (`@WebAuthBasic`)

Checks `Authorization: Basic ...` against `WebBasicAuthParams.getBasicAuthLogin()`/`getBasicAuthPass()`.

## Idempotence

`@WebIdempotence` sends unique request ID (default param `_reqId`). `WebIdempotenceFilter` caches responses, returns cached on duplicate. Configured via `WebIdempotenceParams`. Client auto-generates key.

## Rendering

| `WebRenderType` | Render Class | Description |
|---|---|---|
| `JSON` | `WebJsonRender` | JSON (default) |
| `JSON_PRETTY` | `WebJsonPrettyRender` | Pretty JSON |
| `BASE64_BYTES` | `WebB64Render` | Base64 bytes |
| `RAW_STRING` | `WebRawStringRender` | Raw string |
| `RAW_BYTE_ZIPPED` | `WebRawByteRenderZipped` | Gzipped bytes |

`WebFilterOutput`: `.rawValue(200, obj)`, `.rawProblem(code, problem)`, `.empty()`, `.rendered(result)`.

## Configuration (Spring Properties)

### Server (`WebServerSpringParams`)

| Property | Default | Description |
|---|---|---|
| `web_server_port` | `0` (random) | Server port (0 = auto via `Io.getFreePort()`) |
| `web_server_formlimit` | `1000000` | Max form/multipart size (bytes) |
| `web_server_static_files_location` | null | External static files dir |
| `web_server_static_files_location_resource` | null | Classpath static files |
| `web_server_idle_timeout` | null | Connection idle timeout (ms) |
| `web_server_shutdown_wait` | null | Graceful shutdown wait (ms) |
| `web_server_token_timeout_sec` | null | Token timeout |
| `web_server_token_in_cookies` | `false` | Use cookies for tokens |

### Auth Secrets (`WebSecretSpringProvider`)

| Property | Description |
|---|---|
| `spark_secret` | Single API secret |
| `spark_secret_list` | Multiple secrets separated by `#` (e.g., `s1#s2#s3`) |

Both can be used together. All collected into a set. Separator is `#` (NOT comma).

### DDoS Protection (`WebDdosParams`)

| Method | Default | Description |
|---|---|---|
| `getUserInCourtPeriod()` | 5s | Time window for counting requests |
| `getUserRequestsAllowedInCourt()` | 10 | Max requests per window |
| `getUserInJailTime()` | 5min | Ban duration after exceeding limit |
| `isDdosCourtEnabled()` | `true` (except test) | Master toggle |
| `getDdosPassingHeader()` | `O.empty()` | Header to bypass DDoS checks |

### Error Handling (`WebExceptionParams`)

`getUnhandledJskExceptionHttpCode()` (default 500), `getUnknownExceptionHttpCode()` (default 500), `shouldLog(Exception)`. Override bean or use `getExceptionProcessors()` for per-method handling.

## Graceful Shutdown

`WebServerCore` implements `AppStopListener`. On stop: sets `shouldStop` flag → `WebShutdownFilter` returns 503 for new requests. Override `waitBeforeStopMs()` (default 1000ms) to adjust delay.

## Maven Plugins

### `web-api-info-generator-maven-plugin` (REQUIRED)

Parses API `.java` source with JavaParser → generates metadata JSON to `__jsk_util/api_info/`. **Without this, runtime throws error.** Captures parameter names (not available via reflection), Javadoc, hash codes for API versioning.

```xml
<plugin>
    <artifactId>web-api-info-generator-maven-plugin</artifactId>
    <executions><execution>
        <goals><goal>CREATE_META</goal></goals>
        <configuration>
            <apiClasses><apiClass>com.example.MyApi</apiClass></apiClasses>
        </configuration>
    </execution></executions>
</plugin>
```

### `web-client-swagger-maven-plugin`

Generates OpenAPI 3.0 specs + optional typed clients. Specs saved to `__jsk_util/swagger/api_specs/<ApiClassName>.json`. Served at runtime via `WebServerCoreWithPings` at `{basePath}/api-info-swagger`.

Supported generators: `DART`, `DART_DIO` (custom templates), `DART_DIO_NEXT`, `TYPE_SCRIPT_JQ`, `TYPE_SCRIPT_AXIOS`.

```xml
<plugin>
    <artifactId>web-client-swagger-maven-plugin</artifactId>
    <executions><execution>
        <goals><goal>CREATE_META</goal></goals>
        <configuration>
            <apiClasses><apiClass>com.example.MyApi</apiClass></apiClasses>
            <generators>
                <generator>DART_DIO:/path/to/output</generator>
                <generator>TYPE_SCRIPT_AXIOS:/path/to/output</generator>
            </generators>
        </configuration>
    </execution></executions>
</plugin>
```

## Key Differences from Spring MVC

| Aspect | JSK Web | Spring MVC |
|---|---|---|
| API definition | Interface + annotations | Controller class + `@RequestMapping` |
| Contract sharing | Same interface for client AND server | Separate client (Feign, RestTemplate) |
| Parameter names | Build-time source parsing (Maven plugin) | Reflection `-parameters` or `@RequestParam` |
| HTTP server | Embedded Jetty (Spark-like context) | Embedded Tomcat/Jetty/Undertow |
| DI | Spring IoC (container only) | Spring IoC (deeply integrated) |
| Filters | Custom `WebServerFilter` with priorities | Servlet filters / `HandlerInterceptor` |
| Auto-docs | Built-in HTML + Postman + Swagger | Swagger/SpringDoc addon |
| Client gen | Built-in proxy + OpenAPI codegen | Feign / RestTemplate / WebClient |
| Idempotence | Built-in `@WebIdempotence` | Manual |
| DDoS | Built-in `WebDdosFilter` | Manual / addon |
| Auth | Built-in `@WebAuth` + secret providers | Spring Security |
| Monitoring | JavaMelody integrated | Actuator / Micrometer |

## Key Classes Reference

### Core (web-core)

| Class | Role |
|---|---|
| `WebMethodType` | Enum: `GET`, `POST_MULTI`, `POST_MULTI_SURE`, `POST_FORM`, `POST_BODY` |
| `WebClassInfo` | All metadata for an API class: prefix, methods, DTO classes |
| `WebMethodInfo` | Method metadata: path, HTTP method, parameters, auth, idempotence |
| `WebClassInfoProvider` | Interface to get `WebClassInfo` |
| `WebMethodInfoProviderImpl` | Loads pre-compiled metadata, resolves annotations |
| `WebApiMethod<T>` | Reads annotations from method or class level (with override) |
| `WebUtils` | Path construction, method discovery, type simplification |
| `WebRenderType` | Enum of render types |
| `WebFilterOutput` | Request result (raw value, problem, or pre-rendered) |
| `WebAuthServer` / `WebAuthClient` | Auth interfaces for server/client |
| `WebSecretProvider` | Provides set of valid secrets |
| `O<T>` | JSK's short `Optional<T>` |

### Server (web-server-core)

| Class | Role |
|---|---|
| `WebServerCore<API>` | Main engine: routes, filter chain, method invocation |
| `WebServerCoreWithPings<API>` | Adds `{basePath}/ping`, `/jskinfo`, `/api-info`, `/api-info-postman`, `/api-info-swagger` |
| `WebServerContext` | Route registration abstraction (`addGet`, `addPost`) |
| `WebServerFilter` | Filter interface with priority ordering |
| `WebContextHolder` | Thread-local current request context |
| `WebServerParams` | Server config: port, form limit, static files |
| `WebDdosParams` | DDoS config: court period, requests, jail time |
| `WebExceptionParams` | Exception HTTP codes |
| `WebAdditionalParams` | Hook for extra params per request |

### Server - Jetty (web-server-spark-spring-melody)

| Class | Role |
|---|---|
| `WebJettyServerStarter` | Starts embedded Jetty |
| `WebJettyEntryPoint` | `SpringAppEntryPoint` for Jetty |
| `WebSparkCoreConfig` | Spring config registering all server beans + JavaMelody |
| `WebJettyContextConsumer` | Extension for custom Jetty context config |
| `WebJettyContextConsumer4Spark` | Spark route registration |
| `WebJettyContextConsumer4Melody` | JavaMelody monitoring |

### Client (web-client-core)

| Class | Role |
|---|---|
| `WebClientFactory` | Creates typed HTTP client proxies |
| `WebApiClientExecutionModel` | HTTP request model: URL, method, headers, params |
| `WebRequestResultModel<T>` | HTTP response model |
| `WebClientResultHandler<E>` | Response processing interface |
| `WebClientResultHandlerSimpleJsonImpl` | Default JSON handler |
| `WebClientInputHandler` | Pre-request hook |
| `WebPlatformSpecificHelper` | Platform abstraction (Java proxy vs TeaVM) |

### Swagger

| Class | Role |
|---|---|
| `WebSwaggerGenerator` | Generates OpenAPI 3.0 spec from API metadata |
| `WebSwaggerMavenPlugin` | Build-time: Swagger spec + optional client code |
