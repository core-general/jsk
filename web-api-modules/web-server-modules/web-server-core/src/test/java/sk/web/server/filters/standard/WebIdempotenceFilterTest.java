package sk.web.server.filters.standard;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.services.idempotence.IIdempProvider;
import sk.services.idempotence.IIdentityProvider;
import sk.services.idempotence.IdempLockResult;
import sk.services.idempotence.IdempValue;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;
import sk.web.annotations.WebIdempotence;
import sk.web.renders.WebContentTypeMeta;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRender;
import sk.web.renders.WebRenderResult;
import sk.web.renders.WebReplyMeta;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.context.WebRequestIp;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.filters.WebServerFilterNext;
import sk.web.server.params.WebIdempotenceParams;
import sk.web.utils.WebApiMethod;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebIdempotenceFilterTest {
    private static final String REQUEST_ID = "request:42";
    private static final String REQUEST_HASH = "request-hash";
    private static final WebIdempotence IDEMPOTENCE = TestApi.class.getAnnotation(WebIdempotence.class);

    private final ThreadLocal<String> currentIdentity = new ThreadLocal<>();
    private RecordingIdempotenceProvider idempotence;
    private WebIdempotenceFilter filter;

    @BeforeEach
    void setUp() {
        idempotence = new RecordingIdempotenceProvider();
        filter = new WebIdempotenceFilter();
        filter.idempotence = Optional.of(idempotence);
        filter.identityProvider = Optional.of(() -> O.ofNull(currentIdentity.get()));
        filter.conf = Optional.of(new WebIdempotenceParams() {
            @Override
            public Duration getLockDuration() {
                return Duration.ofMinutes(1);
            }

            @Override
            public Duration getCacheDuration() {
                return Duration.ofHours(1);
            }
        });
        filter.except = () -> null;
    }

    @Test
    void sameRequestIdIsIndependentAcrossIdentitiesAndReplaysWithinEachIdentity() {
        AtomicInteger invocations = new AtomicInteger();

        WebFilterOutput alice = invoke("alice", REQUEST_ID,
                () -> success("alice-" + invocations.incrementAndGet()));
        WebFilterOutput aliceReplay = invoke("alice", REQUEST_ID,
                () -> success("unexpected-" + invocations.incrementAndGet()));
        WebFilterOutput bob = invoke("bob", REQUEST_ID,
                () -> success("bob-" + invocations.incrementAndGet()));
        WebFilterOutput bobReplay = invoke("bob", REQUEST_ID,
                () -> success("unexpected-" + invocations.incrementAndGet()));

        assertEquals("alice-1", alice.getRawOrRenderedAsString());
        assertEquals("alice-1", aliceReplay.getRawOrRenderedAsString());
        assertEquals("bob-2", bob.getRawOrRenderedAsString());
        assertEquals("bob-2", bobReplay.getRawOrRenderedAsString());
        assertEquals(2, invocations.get());
        assertEquals(List.of(scoped("alice", REQUEST_ID), scoped("alice", REQUEST_ID),
                        scoped("bob", REQUEST_ID), scoped("bob", REQUEST_ID)),
                List.copyOf(idempotence.lockKeys));
        assertEquals(List.of(scoped("alice", REQUEST_ID), scoped("bob", REQUEST_ID)),
                List.copyOf(idempotence.cacheKeys));
    }

    @Test
    void lengthPrefixesPreventIdentityAndRequestBoundariesFromColliding() {
        AtomicInteger invocations = new AtomicInteger();

        invoke("a", "b:c", () -> success("first-" + invocations.incrementAndGet()));
        invoke("a:b", "c", () -> success("second-" + invocations.incrementAndGet()));

        assertEquals(2, invocations.get());
        assertNotEquals(scoped("a", "b:c"), scoped("a:b", "c"));
        assertEquals(List.of(scoped("a", "b:c"), scoped("a:b", "c")),
                List.copyOf(idempotence.cacheKeys));
    }

    @Test
    void absentIdentityProviderPreservesTheRawRequestId() {
        filter.identityProvider = Optional.empty();

        invoke(null, REQUEST_ID, () -> success("raw"));

        assertEquals(List.of(REQUEST_ID), List.copyOf(idempotence.lockKeys));
        assertEquals(List.of(REQUEST_ID), List.copyOf(idempotence.cacheKeys));
    }

    @Test
    void emptyIdentityPreservesTheRawRequestId() {
        filter.identityProvider = Optional.of(IIdentityProviderReturningEmpty.INSTANCE);

        invoke(null, REQUEST_ID, () -> success("raw"));

        assertEquals(List.of(REQUEST_ID), List.copyOf(idempotence.lockKeys));
        assertEquals(List.of(REQUEST_ID), List.copyOf(idempotence.cacheKeys));
    }

    @Test
    void problemResponseClearsTheScopedLockAndCanRunAgain() {
        AtomicInteger invocations = new AtomicInteger();

        WebFilterOutput problem = invoke("alice", REQUEST_ID, () -> {
            invocations.incrementAndGet();
            return rendered(409, true, "problem");
        });
        WebFilterOutput retry = invoke("alice", REQUEST_ID, () -> {
            invocations.incrementAndGet();
            return success("recovered");
        });

        assertEquals(409, problem.getCode());
        assertEquals("recovered", retry.getRawOrRenderedAsString());
        assertEquals(2, invocations.get());
        assertEquals(List.of(scoped("alice", REQUEST_ID)), List.copyOf(idempotence.unlockKeys));
        assertEquals(List.of(scoped("alice", REQUEST_ID)), List.copyOf(idempotence.cacheKeys));
    }

    @Test
    void exceptionClearsTheScopedLockAndRethrowsTheSameFailure() {
        RuntimeException failure = new RuntimeException("handler failed");

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> invoke("alice", REQUEST_ID, () -> {
                    throw failure;
                }));
        WebFilterOutput retry = invoke("alice", REQUEST_ID, () -> success("recovered"));

        assertSame(failure, thrown);
        assertEquals("recovered", retry.getRawOrRenderedAsString());
        assertEquals(List.of(scoped("alice", REQUEST_ID)), List.copyOf(idempotence.unlockKeys));
    }

    @Test
    void concurrentUsersCanAcquireTheSameRawRequestIdIndependently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothHandlersStarted = new CountDownLatch(2);
        CountDownLatch releaseHandlers = new CountDownLatch(1);
        try {
            Future<WebFilterOutput> alice = executor.submit(() -> invoke("alice", REQUEST_ID,
                    () -> waitingSuccess("alice", bothHandlersStarted, releaseHandlers)));
            Future<WebFilterOutput> bob = executor.submit(() -> invoke("bob", REQUEST_ID,
                    () -> waitingSuccess("bob", bothHandlersStarted, releaseHandlers)));

            assertTrue(bothHandlersStarted.await(5, TimeUnit.SECONDS));
            releaseHandlers.countDown();

            assertEquals("alice", alice.get(5, TimeUnit.SECONDS).getRawOrRenderedAsString());
            assertEquals("bob", bob.get(5, TimeUnit.SECONDS).getRawOrRenderedAsString());
            assertTrue(idempotence.contains(scoped("alice", REQUEST_ID)));
            assertTrue(idempotence.contains(scoped("bob", REQUEST_ID)));
        } finally {
            releaseHandlers.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentDuplicateForOneIdentityDoesNotEnterTheHandlerTwice() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstHandlerStarted = new CountDownLatch(1);
        CountDownLatch releaseHandler = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger();
        try {
            Future<WebFilterOutput> first = executor.submit(() -> invoke("alice", REQUEST_ID, () -> {
                invocations.incrementAndGet();
                firstHandlerStarted.countDown();
                await(releaseHandler);
                return success("first");
            }));
            assertTrue(firstHandlerStarted.await(5, TimeUnit.SECONDS));

            WebFilterOutput duplicate = invoke("alice", REQUEST_ID, () -> {
                invocations.incrementAndGet();
                return success("duplicate");
            });

            assertEquals(503, duplicate.getCode());
            assertEquals(1, invocations.get());
            releaseHandler.countDown();
            assertEquals("first", first.get(5, TimeUnit.SECONDS).getRawOrRenderedAsString());
        } finally {
            releaseHandler.countDown();
            executor.shutdownNow();
        }
    }

    private WebFilterOutput invoke(String identity, String requestId, WebServerFilterNext next) {
        if (identity == null) {
            currentIdentity.remove();
        } else {
            currentIdentity.set(identity);
        }
        try {
            WebRequestIp ip = new WebRequestIp("127.0.0.1", List.of(), O.empty());
            WebRequestInnerContext request = (WebRequestInnerContext) Proxy.newProxyInstance(
                    WebIdempotenceFilterTest.class.getClassLoader(),
                    new Class<?>[]{WebRequestInnerContext.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getWebIdempotence" -> O.of(IDEMPOTENCE);
                        case "getParamAsString" -> O.of(requestId);
                        case "getRequestHash" -> REQUEST_HASH;
                        case "getServerRequestId" -> "server-request";
                        case "getUserToken" -> O.empty();
                        case "getIpInfo" -> ip;
                        case "getUrlPathPart" -> "/test";
                        case "getStartTime" -> ZonedDateTime.of(2026, 7, 20, 0, 0, 0, 0, ZoneOffset.UTC);
                        case "getWebRender" -> PassThroughRender.INSTANCE;
                        case "toString" -> "idempotence-test-request";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            WebApiMethod<TestApi> method = new WebApiMethod<>(TestApi.class, O.empty(), false);
            return filter.invoke(new WebServerFilterContext<>(method, request, next));
        } finally {
            currentIdentity.remove();
        }
    }

    private static WebFilterOutput waitingSuccess(
            String value, CountDownLatch started, CountDownLatch release) {
        started.countDown();
        await(release);
        return success(value);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static WebFilterOutput success(String value) {
        return rendered(200, false, value);
    }

    private static WebFilterOutput rendered(int code, boolean problem, String value) {
        return WebFilterOutput.rendered(new WebRenderResult(
                new WebReplyMeta(code, new WebContentTypeMeta("application/json"), false, problem),
                OneOf.left(value)));
    }

    private static String scoped(String identity, String requestId) {
        return identity.length() + ":" + identity + ":" + requestId.length() + ":" + requestId;
    }

    @WebIdempotence
    private interface TestApi {}

    private enum IIdentityProviderReturningEmpty implements IIdentityProvider {
        INSTANCE;

        @Override
        public O<String> currentIdentity() {
            return O.empty();
        }
    }

    private enum PassThroughRender implements WebRender {
        INSTANCE;

        @Override
        public WebContentTypeMeta contentHeaderProvider(
                Object value, OneOf<String, byte[]> processed, WebApiMethod<?> method) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean allowDeflation(Object value, OneOf<String, byte[]> processed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OneOf<String, byte[]> valueProvider(Object value, WebApiMethod<?> method) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingIdempotenceProvider implements IIdempProvider {
        private final ConcurrentHashMap<String, StoredValue> values = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<String> lockKeys = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> cacheKeys = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> unlockKeys = new ConcurrentLinkedQueue<>();

        @Override
        public <META> IdempLockResult<META> tryLock(
                String key, String requestHash, TypeWrap<META> meta, Duration lockDuration,
                O<String> additionalData4Lock) {
            lockKeys.add(key);
            AtomicReference<IdempLockResult<META>> result = new AtomicReference<>();
            values.compute(key, (ignored, existing) -> {
                if (existing == null) {
                    result.set(IdempLockResult.lockOk());
                    return new StoredValue(requestHash, null);
                }
                if (!existing.requestHash.equals(requestHash)) {
                    result.set(IdempLockResult.badParams());
                    return existing;
                }
                if (existing.value == null) {
                    result.set(IdempLockResult.lockBad());
                    return existing;
                }
                @SuppressWarnings("unchecked")
                IdempValue<META> cached = (IdempValue<META>) existing.value;
                result.set(IdempLockResult.cachedValue(cached));
                return existing;
            });
            return result.get();
        }

        @Override
        public <META> void cacheValue(
                String key, String requestHash, IdempValue<META> valueToCache, Duration cacheDuration) {
            cacheKeys.add(key);
            values.put(key, new StoredValue(requestHash, valueToCache));
        }

        @Override
        public void unlockOrClear(String key) {
            unlockKeys.add(key);
            values.remove(key);
        }

        private boolean contains(String key) {
            return values.containsKey(key);
        }
    }

    private record StoredValue(String requestHash, IdempValue<?> value) {}
}
