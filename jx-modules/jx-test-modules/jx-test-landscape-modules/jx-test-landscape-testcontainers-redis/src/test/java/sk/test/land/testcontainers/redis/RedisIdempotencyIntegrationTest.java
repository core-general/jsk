package sk.test.land.testcontainers.redis;

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

import org.junit.jupiter.api.*;
import sk.redis.RedisKVStoreImpl;
import sk.services.bytes.BytesImpl;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.idempotence.*;
import sk.services.json.JGsonImpl;
import sk.services.log.ILog;
import sk.services.log.ILogCategory;
import sk.services.log.ILogSeverity;
import sk.services.log.ILogType;
import sk.services.profile.IAppProfile;
import sk.services.profile.IAppProfileType;
import sk.services.time.ITime;
import sk.services.time.TimeUtcImpl;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: {@link IIdempProviderUnlimitedKV} backed by {@link RedisKVStoreImpl}.
 * <p>
 * Verifies the full idempotency lifecycle through the Redis KV store:
 * <ul>
 *   <li>tryLock → cacheValue → tryLock (returns cached)</li>
 *   <li>Lock conflict (lockBad)</li>
 *   <li>Hash mismatch (badParams)</li>
 *   <li>unlockOrClear → re-lock</li>
 *   <li>Binary response body round-trip</li>
 *   <li>ZIP compression for large string bodies</li>
 * </ul>
 * <p>
 * Requires Docker to be available (Testcontainers). When Docker is absent, tests will
 * fail at container startup — expected in environments without Docker.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisIdempotencyIntegrationTest {

    static final int REDIS_PORT = 16380; // Different port from RedisKVStoreIntegrationTest (16379)
    static final String KEY_PREFIX = "test_idemp";

    static JskLandRedis redisLand;
    static IIdempProvider idempProvider;
    static ITime times;
    static JGsonImpl json;

    @BeforeAll
    static void setup() throws Exception {
        redisLand = new JskLandRedis(REDIS_PORT, KEY_PREFIX);

        times = new TimeUtcImpl();

        // Build JGsonImpl manually with minimal deps (same pattern as RedisKVStoreIntegrationTest)
        json = new JGsonImpl(false);
        IBytes bytesImpl = new BytesImpl();
        setField(json, "times", times);
        setField(json, "bytes", bytesImpl);
        json.init();

        // Build RedisKVStoreImpl
        RedisKVStoreImpl kvStore = new RedisKVStoreImpl();
        setField(kvStore, "redis", redisLand.getConnectionProvider());
        setField(kvStore, "properties", redisLand.getRedisProperties());
        setField(kvStore, "times", times);
        setField(kvStore, "json", json);
        setField(kvStore, "appProfile", testProfile(false));
        kvStore.init();

        // Build IIdempProviderUnlimitedKV with all its @Inject dependencies
        IIdempProviderUnlimitedKV provider = new IIdempProviderUnlimitedKV();
        setField(provider, "kv", kvStore);
        setField(provider, "times", times);
        setField(provider, "json", json);
        setField(provider, "bytes", bytesImpl);
        setField(provider, "except", createExcept());
        setField(provider, "log", createLog());
        setField(provider, "config", Optional.empty());

        idempProvider = provider;
    }

    @AfterAll
    static void teardown() throws Exception {
        if (redisLand != null) {
            redisLand.stop();
        }
    }

    @BeforeEach
    void clearRedis() {
        redisLand.toEmptyState();
    }

    // =============================================
    // Full Lifecycle: lock → cache → read cached
    // =============================================

    @Test
    @Order(1)
    void lifecycle_lockThenCacheThenReadCached() {
        String key = "lifecycle-001";
        String requestHash = "hash-abc-123";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Step 1: First tryLock → should succeed (lockOk)
        IdempLockResult<String> lockResult = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));

        assertTrue(lockResult.getValueOrLockSuccessStatus().isRight(),
                "First tryLock should return lock status (right)");
        assertTrue(lockResult.getValueOrLockSuccessStatus().right(),
                "Lock should be acquired (true)");

        // Step 2: Cache a string value
        String cachedMeta = "response-metadata";
        String cachedBody = "HTTP 200 response body";
        idempProvider.cacheValue(key, requestHash,
                new IdempValue<>(cachedMeta, OneOf.left(cachedBody)),
                Duration.ofMinutes(5));

        // Step 3: Second tryLock with same key+hash → should return cached value
        IdempLockResult<String> cachedResult = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));

        assertTrue(cachedResult.getValueOrLockSuccessStatus().isLeft(),
                "Second tryLock should return cached value (left)");
        O<IdempValue<String>> cached = cachedResult.getValueOrLockSuccessStatus().left();
        assertTrue(cached.isPresent(), "Cached value should be present (not badParams)");

        IdempValue<String> idempValue = cached.get();
        assertEquals(cachedMeta, idempValue.getMetainfo(),
                "Metainfo should match what was cached");
        assertTrue(idempValue.getCachedValue().isLeft(),
                "Cached body should be string (left)");
        assertEquals(cachedBody, idempValue.getCachedValue().left(),
                "Cached body should match exactly");
    }

    // =============================================
    // Lock conflict: lockBad when another request holds the lock
    // =============================================

    @Test
    @Order(2)
    void lockBad_whenLockHeldAndNotYetCached() {
        String key = "lockbad-001";
        String requestHash = "hash-req-1";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // First request acquires lock
        IdempLockResult<String> lock1 = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));
        assertTrue(lock1.getValueOrLockSuccessStatus().isRight());
        assertTrue(lock1.getValueOrLockSuccessStatus().right(), "First lock should succeed");

        // Second request with SAME hash tries to lock → lockBad (lock held, not yet cached)
        // IIdempProviderUnlimitedKV checks: if metaData.isLockSign() → lockBad (right(false))
        IdempLockResult<String> lock2 = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));

        assertTrue(lock2.getValueOrLockSuccessStatus().isRight(),
                "lockBad should return right (lock status)");
        assertFalse(lock2.getValueOrLockSuccessStatus().right(),
                "lockBad should be false (lock is held by someone else)");
    }

    // =============================================
    // Hash mismatch: badParams when different hash for same key
    // =============================================

    @Test
    @Order(3)
    void badParams_whenDifferentHashForSameKey() {
        String key = "badparams-001";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // First request acquires lock and caches with hash "hash-A"
        idempProvider.tryLock(key, "hash-A", metaType, Duration.ofSeconds(30));
        idempProvider.cacheValue(key, "hash-A",
                new IdempValue<>("meta", OneOf.left("body")),
                Duration.ofMinutes(5));

        // Second request with DIFFERENT hash "hash-B" → badParams
        // The stored requestHash is "hash-A", requesting with "hash-B" → hash mismatch
        IdempLockResult<String> result = idempProvider.tryLock(
                key, "hash-B", metaType, Duration.ofSeconds(30));

        OneOf<O<IdempValue<String>>, Boolean> status = result.getValueOrLockSuccessStatus();
        assertTrue(status.isLeft(), "badParams returns left (value side)");
        assertFalse(status.left().isPresent(),
                "badParams returns empty Optional — different hash means bad params");
    }

    @Test
    @Order(4)
    void badParams_whenDifferentHashForLockedKey() {
        String key = "badparams-lock-001";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // First request acquires lock with hash "hash-A" (no cacheValue call — still locked)
        idempProvider.tryLock(key, "hash-A", metaType, Duration.ofSeconds(30));

        // Second request with DIFFERENT hash "hash-B" → badParams
        // Even while locked, hash mismatch is checked first in IIdempProviderUnlimitedKV
        IdempLockResult<String> result = idempProvider.tryLock(
                key, "hash-B", metaType, Duration.ofSeconds(30));

        OneOf<O<IdempValue<String>>, Boolean> status = result.getValueOrLockSuccessStatus();
        assertTrue(status.isLeft(), "badParams returns left");
        assertFalse(status.left().isPresent(), "badParams returns empty O");
    }

    // =============================================
    // unlockOrClear: removes lock and allows re-lock
    // =============================================

    @Test
    @Order(5)
    void unlockOrClear_removesLockAndAllowsRelock() {
        String key = "unlock-001";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Acquire lock
        IdempLockResult<String> lock1 = idempProvider.tryLock(
                key, "hash-1", metaType, Duration.ofSeconds(30));
        assertTrue(lock1.getValueOrLockSuccessStatus().right());

        // Verify lock is held (lockBad)
        IdempLockResult<String> lockCheck = idempProvider.tryLock(
                key, "hash-1", metaType, Duration.ofSeconds(30));
        assertTrue(lockCheck.getValueOrLockSuccessStatus().isRight());
        assertFalse(lockCheck.getValueOrLockSuccessStatus().right(), "Lock should be held");

        // Clear lock
        idempProvider.unlockOrClear(key);

        // New lock should succeed (key was cleared)
        IdempLockResult<String> lock2 = idempProvider.tryLock(
                key, "hash-2", metaType, Duration.ofSeconds(30));

        assertTrue(lock2.getValueOrLockSuccessStatus().isRight(),
                "After clear, tryLock should return lock status");
        assertTrue(lock2.getValueOrLockSuccessStatus().right(),
                "After clear, new lock should succeed");
    }

    @Test
    @Order(6)
    void unlockOrClear_removesCachedValueAllowsRelock() {
        String key = "unlock-cached-001";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Lock + cache
        idempProvider.tryLock(key, "hash-old", metaType, Duration.ofSeconds(30));
        idempProvider.cacheValue(key, "hash-old",
                new IdempValue<>("old-meta", OneOf.left("old-body")),
                Duration.ofMinutes(5));

        // Clear
        idempProvider.unlockOrClear(key);

        // Re-lock with different hash should succeed (key was cleared completely)
        IdempLockResult<String> newLock = idempProvider.tryLock(
                key, "hash-new", metaType, Duration.ofSeconds(30));

        assertTrue(newLock.getValueOrLockSuccessStatus().isRight());
        assertTrue(newLock.getValueOrLockSuccessStatus().right(),
                "After unlockOrClear, should be able to re-lock with new hash");
    }

    // =============================================
    // Binary response body round-trip
    // =============================================

    @Test
    @Order(7)
    void lifecycle_withBinaryResponseBody() {
        String key = "binary-001";
        String requestHash = "hash-binary";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Lock
        idempProvider.tryLock(key, requestHash, metaType, Duration.ofSeconds(30));

        // Cache with binary response body (BYTEARR_SIGN 'B')
        byte[] binaryBody = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryBody[i] = (byte) i;
        }
        idempProvider.cacheValue(key, requestHash,
                new IdempValue<>("binary-meta", OneOf.right(binaryBody)),
                Duration.ofMinutes(5));

        // Read cached value
        IdempLockResult<String> result = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));

        O<IdempValue<String>> cached = result.getValueOrLockSuccessStatus().left();
        assertTrue(cached.isPresent());
        assertEquals("binary-meta", cached.get().getMetainfo());
        assertTrue(cached.get().getCachedValue().isRight(),
                "Binary body should come back as right (byte[])");
        assertArrayEquals(binaryBody, cached.get().getCachedValue().right(),
                "All 256 byte values must survive the round-trip");
    }

    // =============================================
    // ZIP compression: large string body (>800 chars)
    // =============================================

    @Test
    @Order(8)
    void lifecycle_withLargeStringBody_usesZipCompression() {
        String key = "zip-001";
        String requestHash = "hash-zip";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Lock
        idempProvider.tryLock(key, requestHash, metaType, Duration.ofSeconds(30));

        // Cache with large string body (>800 chars triggers ZIP_SLOW_SIGN 'Z' in IIdempProviderUnlimitedKV)
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeBody.append("This is line ").append(i).append(" of a large response body.\n");
        }
        String largeString = largeBody.toString();
        assertTrue(largeString.length() > 800,
                "Test precondition: body must be >800 chars to trigger ZIP compression");

        idempProvider.cacheValue(key, requestHash,
                new IdempValue<>("zip-meta", OneOf.left(largeString)),
                Duration.ofMinutes(5));

        // Read cached value
        IdempLockResult<String> result = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));

        O<IdempValue<String>> cached = result.getValueOrLockSuccessStatus().left();
        assertTrue(cached.isPresent());
        assertEquals("zip-meta", cached.get().getMetainfo());
        assertTrue(cached.get().getCachedValue().isLeft(),
                "Decompressed body should come back as string (left)");
        assertEquals(largeString, cached.get().getCachedValue().left(),
                "Large body must survive zip/unzip round-trip through Redis");
    }

    // =============================================
    // Small string body (<= 800 chars): STRING_SIGN path
    // =============================================

    @Test
    @Order(9)
    void lifecycle_withSmallStringBody_usesStringEncoding() {
        String key = "string-001";
        String requestHash = "hash-string";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Lock
        idempProvider.tryLock(key, requestHash, metaType, Duration.ofSeconds(30));

        // Cache with small string body (<=800 chars → STRING_SIGN 'S')
        String smallBody = "Short response body";
        assertTrue(smallBody.length() <= 800, "Test precondition: body <= 800 chars");

        idempProvider.cacheValue(key, requestHash,
                new IdempValue<>("string-meta", OneOf.left(smallBody)),
                Duration.ofMinutes(5));

        // Read cached value
        IdempLockResult<String> result = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));

        O<IdempValue<String>> cached = result.getValueOrLockSuccessStatus().left();
        assertTrue(cached.isPresent());
        assertEquals("string-meta", cached.get().getMetainfo());
        assertTrue(cached.get().getCachedValue().isLeft());
        assertEquals(smallBody, cached.get().getCachedValue().left());
    }

    // =============================================
    // Complex meta type
    // =============================================

    @Test
    @Order(10)
    void lifecycle_withComplexMetaType() {
        String key = "complex-meta-001";
        String requestHash = "hash-complex";
        TypeWrap<int[]> metaType = TypeWrap.simple(int[].class);

        // Lock
        IdempLockResult<int[]> lockResult = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));
        assertTrue(lockResult.getValueOrLockSuccessStatus().right());

        // Cache with int[] meta
        int[] metaArray = {200, 42, 99};
        idempProvider.cacheValue(key, requestHash,
                new IdempValue<>(metaArray, OneOf.left("complex-body")),
                Duration.ofMinutes(5));

        // Read cached value
        IdempLockResult<int[]> result = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));

        O<IdempValue<int[]>> cached = result.getValueOrLockSuccessStatus().left();
        assertTrue(cached.isPresent());
        assertArrayEquals(metaArray, cached.get().getMetainfo(),
                "Complex meta (int[]) should survive JSON serialization round-trip");
        assertEquals("complex-body", cached.get().getCachedValue().left());
    }

    // =============================================
    // Multiple independent keys
    // =============================================

    @Test
    @Order(11)
    void multipleKeys_independentLifecycles() {
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Key A: lock + cache
        idempProvider.tryLock("key-A", "hash-A", metaType, Duration.ofSeconds(30));
        idempProvider.cacheValue("key-A", "hash-A",
                new IdempValue<>("meta-A", OneOf.left("body-A")), Duration.ofMinutes(5));

        // Key B: lock only (still locked)
        idempProvider.tryLock("key-B", "hash-B", metaType, Duration.ofSeconds(30));

        // Key A: read cached — should work
        IdempLockResult<String> resultA = idempProvider.tryLock(
                "key-A", "hash-A", metaType, Duration.ofSeconds(30));
        assertTrue(resultA.getValueOrLockSuccessStatus().isLeft());
        assertTrue(resultA.getValueOrLockSuccessStatus().left().isPresent());
        assertEquals("body-A", resultA.getValueOrLockSuccessStatus().left().get().getCachedValue().left());

        // Key B: still locked — should be lockBad
        IdempLockResult<String> resultB = idempProvider.tryLock(
                "key-B", "hash-B", metaType, Duration.ofSeconds(30));
        assertTrue(resultB.getValueOrLockSuccessStatus().isRight());
        assertFalse(resultB.getValueOrLockSuccessStatus().right(), "Key B should still be locked");

        // Key C: new key — should lock ok
        IdempLockResult<String> resultC = idempProvider.tryLock(
                "key-C", "hash-C", metaType, Duration.ofSeconds(30));
        assertTrue(resultC.getValueOrLockSuccessStatus().isRight());
        assertTrue(resultC.getValueOrLockSuccessStatus().right(), "Key C should lock successfully");
    }

    // =============================================
    // Additional data for lock
    // =============================================

    @Test
    @Order(12)
    void lifecycle_withAdditionalData() {
        String key = "additional-data-001";
        String requestHash = "hash-extra";
        TypeWrap<String> metaType = TypeWrap.simple(String.class);

        // Lock with additional data
        IdempLockResult<String> lockResult = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30),
                O.of("caller=test-suite"));

        assertTrue(lockResult.getValueOrLockSuccessStatus().isRight());
        assertTrue(lockResult.getValueOrLockSuccessStatus().right());

        // Cache and verify round-trip still works
        idempProvider.cacheValue(key, requestHash,
                new IdempValue<>("ad-meta", OneOf.left("ad-body")),
                Duration.ofMinutes(5));

        IdempLockResult<String> result = idempProvider.tryLock(
                key, requestHash, metaType, Duration.ofSeconds(30));
        assertTrue(result.getValueOrLockSuccessStatus().left().isPresent());
        assertEquals("ad-body", result.getValueOrLockSuccessStatus().left().get().getCachedValue().left());
    }

    // =============================================
    // Helpers
    // =============================================

    private static IAppProfile<?> testProfile(boolean isProduction) {
        return new IAppProfile<IAppProfileType>() {
            @Override
            public IAppProfileType getProfile() {
                return new IAppProfileType() {
                    @Override
                    public String name() { return isProduction ? "PROD" : "TEST"; }

                    @Override
                    public boolean isForProductionUsage() { return isProduction; }

                    @Override
                    public boolean isForDefaultTesting() { return !isProduction; }
                };
            }
        };
    }

    private static IExcept createExcept() {
        // IExcept extends IExceptBase. All methods in IExcept are default (throw JskProblemException).
        // IExceptBase.haveStackTrace() is also default (returns false).
        // Override haveStackTrace to true for better test diagnostics.
        return new IExcept() {
            @Override
            public boolean haveStackTrace(Class<? extends sk.exceptions.JskProblemException> exceptionCls) {
                return true;
            }
        };
    }

    private static ILog createLog() {
        // ILog has one abstract method: uni(severity, category, type, info, logType)
        // All other methods (logError, logExc, etc.) are defaults that call uni().
        return new ILog() {
            @Override
            public void uni(ILogSeverity severity, ILogCategory category, String type,
                    Map<String, Object> info, ILogType logType) {
                System.out.println("[" + severity + "] " + category.name() + " " + type + ": " + info);
            }
        };
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> cls = target.getClass();
            while (cls != null) {
                try {
                    java.lang.reflect.Field field = cls.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
