package sk.services.idempotence;

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
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.services.async.IAsync;
import sk.services.async.ISizedSemaphore;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.free.IFree;
import sk.services.http.IHttp;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.kv.KvAllValues;
import sk.services.kv.IKvLocal4Test;
import sk.services.kv.IKvUnlimitedStore;
import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.log.ILog;
import sk.services.rand.IRand;
import sk.services.rescache.IResCache;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sk.utils.functional.OneOf.right;
import static sk.utils.javafixes.TypeWrap.simple;

class IIdempProviderUnlimitedKVTest {
    private MutableTime time;
    private IIdempProviderUnlimitedKV provider;

    @BeforeEach
    void setUp() {
        time = new MutableTime(1_700_000_000_000L);
        ICoreServices raw = CoreServicesRaw.services();
        ICoreServices services = services(raw, time);
        provider = provider(new IKvLocal4Test(raw.json(), time), services);
    }

    @Test
    void replaysSuccessfulResponseAfterCallerLosesFirstResponse() {
        IdempLockResult<String> first = provider.tryLock("operation", "payload", simple(String.class),
                Duration.ofMinutes(1));
        assertLock(first, true);

        byte[] body = {3, 1, 4};
        provider.cacheValue("operation", "payload", new IdempValue<>("http-meta", right(body)),
                Duration.ofHours(3));

        IdempValue<String> replay = provider.tryLock("operation", "payload", simple(String.class),
                        Duration.ofMinutes(1))
                .getValueOrLockSuccessStatus().left().get();
        assertEquals("http-meta", replay.getMetainfo());
        assertArrayEquals(body, replay.getCachedValue().right());
    }

    @Test
    void onlyOneConcurrentDuplicateAcquiresTheLock() throws Exception {
        try (var pool = Executors.newFixedThreadPool(8)) {
            Callable<IdempLockResult<String>> contender = () -> provider.tryLock(
                    "concurrent", "payload", simple(String.class), Duration.ofMinutes(1));
            var futures = pool.invokeAll(java.util.Collections.nCopies(32, contender));
            int acquired = 0;
            int retry = 0;
            for (var future : futures) {
                OneOf<sk.utils.functional.O<IdempValue<String>>, Boolean> result =
                        future.get().getValueOrLockSuccessStatus();
                if (result.right()) acquired++;
                else retry++;
            }
            assertEquals(1, acquired);
            assertEquals(31, retry);
        }
    }

    @Test
    void rejectsReusingAKeyForDifferentPayload() {
        assertLock(provider.tryLock("operation", "payload-a", simple(String.class), Duration.ofMinutes(1)), true);
        IdempLockResult<String> mismatch = provider.tryLock(
                "operation", "payload-b", simple(String.class), Duration.ofMinutes(1));
        assertTrue(mismatch.getValueOrLockSuccessStatus().isLeft());
        assertTrue(mismatch.getValueOrLockSuccessStatus().left().isEmpty());
    }

    @Test
    void expiresBothLocksAndCachedResponses() {
        assertLock(provider.tryLock("lock", "payload", simple(String.class), Duration.ofSeconds(1)), true);
        time.advance(Duration.ofSeconds(2));
        assertLock(provider.tryLock("lock", "payload", simple(String.class), Duration.ofSeconds(1)), true);

        assertLock(provider.tryLock("cache", "payload", simple(String.class), Duration.ofSeconds(1)), true);
        provider.cacheValue("cache", "payload", new IdempValue<>("meta", OneOf.left("response")),
                Duration.ofSeconds(1));
        time.advance(Duration.ofSeconds(2));
        assertLock(provider.tryLock("cache", "payload", simple(String.class), Duration.ofSeconds(1)), true);
    }

    @Test
    void providerFailureIsVisibleBeforeMutationCanRun() {
        ICoreServices raw = CoreServicesRaw.services();
        IKvUnlimitedStore failed = new IKvLocal4Test(raw.json(), time) {
            @Override
            public synchronized OneOf<Boolean, Exception> trySaveNewStringAndRaw(
                    KvKey key, KvAllValues<String> newValueProvider) {
                return OneOf.right(new RuntimeException("redis unavailable"));
            }
        };
        IIdempProviderUnlimitedKV unavailable = provider(failed, services(raw, time));

        RuntimeException error = assertThrows(RuntimeException.class, () -> unavailable.tryLock(
                "operation", "payload", simple(String.class), Duration.ofMinutes(1)));
        assertEquals("idempotence_lock_failed", error.getMessage());
        assertEquals("redis unavailable", error.getCause().getMessage());
    }

    @Test
    void cacheWriteFailureLeavesTheOriginalLockUntilItExpires() {
        ICoreServices raw = CoreServicesRaw.services();
        AtomicBoolean failUpdates = new AtomicBoolean();
        IKvUnlimitedStore failedCache = new IKvLocal4Test(raw.json(), time) {
            @Override
            public synchronized OneOf<O<KvAllValues<String>>, Exception> updateStringAndRaw(
                    KvKeyWithDefault key, F1<KvAllValues<String>, O<KvAllValues<String>>> updater) {
                return failUpdates.get()
                        ? OneOf.right(new RuntimeException("redis unavailable during cache write"))
                        : super.updateStringAndRaw(key, updater);
            }
        };
        IIdempProviderUnlimitedKV cacheUnavailable = provider(failedCache, services(raw, time));
        assertLock(cacheUnavailable.tryLock(
                "operation", "payload", simple(String.class), Duration.ofSeconds(1)), true);

        failUpdates.set(true);
        assertDoesNotThrow(() -> cacheUnavailable.cacheValue(
                "operation", "payload", new IdempValue<>("meta", OneOf.left("response")), Duration.ofHours(1)));
        assertFalse(cacheUnavailable.tryLock("operation", "payload", simple(String.class), Duration.ofSeconds(1))
                .getValueOrLockSuccessStatus().right());

        failUpdates.set(false);
        time.advance(Duration.ofSeconds(2));
        assertLock(cacheUnavailable.tryLock(
                "operation", "payload", simple(String.class), Duration.ofSeconds(1)), true);
    }

    private static ICoreServices services(ICoreServices raw, ITime time) {
        return new ServicesWithTime(raw, time);
    }

    private static IIdempProviderUnlimitedKV provider(IKvUnlimitedStore store, ICoreServices services) {
        IIdempProviderUnlimitedKV provider = new IIdempProviderUnlimitedKV();
        provider.kv = store;
        provider.times = services.times();
        provider.json = services.json();
        provider.bytes = services.bytes();
        provider.except = services.except();
        provider.log = services.iLog();
        provider.config = Optional.empty();
        return provider;
    }

    private static void assertLock(IdempLockResult<?> result, boolean expected) {
        assertTrue(result.getValueOrLockSuccessStatus().isRight());
        assertEquals(expected, result.getValueOrLockSuccessStatus().right());
    }

    private static final class MutableTime implements ITime {
        private long epochMs;

        private MutableTime(long epochMs) {
            this.epochMs = epochMs;
        }

        void advance(Duration duration) {
            epochMs += duration.toMillis();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public long now() {
            return epochMs;
        }

        @Override
        public ZonedDateTime nowZ() {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC);
        }
    }

    private record ServicesWithTime(ICoreServices delegate, ITime times) implements ICoreServices {
        @Override public IAsync async() { return delegate.async(); }
        @Override public IBytes bytes() { return delegate.bytes(); }
        @Override public IHttp http() { return delegate.http(); }
        @Override public IIds ids() { return delegate.ids(); }
        @Override public IJson json() { return delegate.json(); }
        @Override public ILog iLog() { return delegate.iLog(); }
        @Override public IRand rand() { return delegate.rand(); }
        @Override public IResCache resCache() { return delegate.resCache(); }
        @Override public IRepeat repeat() { return delegate.repeat(); }
        @Override public IFree free() { return delegate.free(); }
        @Override public IExcept except() { return delegate.except(); }
        @Override public ISizedSemaphore sizedSemaphore() { return delegate.sizedSemaphore(); }
    }
}
