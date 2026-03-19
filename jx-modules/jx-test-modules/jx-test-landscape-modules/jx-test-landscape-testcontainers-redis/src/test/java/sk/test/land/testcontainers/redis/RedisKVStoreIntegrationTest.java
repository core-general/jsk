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
import sk.redis.RedisConnectionProvider;
import sk.redis.RedisKVStoreImpl;
import sk.services.bytes.BytesImpl;
import sk.services.json.JGsonImpl;
import sk.services.kv.*;
import sk.services.kv.keys.KvSimpleKeyWithName;
import sk.services.profile.IAppProfile;
import sk.services.profile.IAppProfileType;
import sk.services.time.ITime;
import sk.services.time.TimeUtcImpl;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RedisKVStoreImpl} using Testcontainers.
 * <p>
 * Requires Docker to be available. When Docker is absent, all tests will fail
 * at container startup (expected in environments without Docker).
 * <p>
 * Covers: CRUD operations, TTL, binary rawValue, concurrency/atomicity.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisKVStoreIntegrationTest {

    static final int REDIS_PORT = 16379;
    static final String KEY_PREFIX = "test_kv";

    static JskLandRedis redisLand;
    static RedisKVStoreImpl kvStore;
    static ITime times;

    @BeforeAll
    static void setup() throws Exception {
        redisLand = new JskLandRedis(REDIS_PORT, KEY_PREFIX);

        times = new TimeUtcImpl();

        // Build JGsonImpl manually with minimal deps
        JGsonImpl json = new JGsonImpl(false);
        setField(json, "times", times);
        setField(json, "bytes", new BytesImpl());
        json.init();

        // Build RedisKVStoreImpl manually (no Spring context in test)
        kvStore = new RedisKVStoreImpl();

        RedisConnectionProvider connectionProvider = redisLand.getConnectionProvider();
        setField(kvStore, "redis", connectionProvider);
        setField(kvStore, "properties", redisLand.getRedisProperties());
        setField(kvStore, "times", times);
        setField(kvStore, "json", json);
        setField(kvStore, "appProfile", testProfile(false));

        // Call @PostConstruct — loads Lua scripts into Redis
        kvStore.init();
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
    // trySaveNewStringAndRaw — Create
    // =============================================

    @Test
    @Order(1)
    void trySaveNew_createsNewKey() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_key1", "default");
        KvAllValues<String> vals = new KvAllValues<>("hello", O.empty(), O.empty());

        OneOf<Boolean, Exception> result = kvStore.trySaveNewStringAndRaw(key, vals);

        assertTrue(result.isLeft());
        assertTrue(result.left());
    }

    @Test
    @Order(2)
    void trySaveNew_returnsFalseIfKeyAlreadyExists() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_key1", "default");
        KvAllValues<String> vals = new KvAllValues<>("hello", O.empty(), O.empty());

        kvStore.trySaveNewStringAndRaw(key, vals);
        OneOf<Boolean, Exception> result = kvStore.trySaveNewStringAndRaw(key, vals);

        assertTrue(result.isLeft());
        assertFalse(result.left(), "Second save should return false (key exists)");
    }

    @Test
    @Order(3)
    void trySaveNew_withTTL_storesTtlAndRedisExpiry() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_ttlkey", "default");
        ZonedDateTime ttl = times.nowZ().plusMinutes(5);
        KvAllValues<String> vals = new KvAllValues<>("hello", O.empty(), O.of(ttl));

        kvStore.trySaveNewStringAndRaw(key, vals);

        O<KvVersionedItem<String>> item = kvStore.getRawVersioned(key);
        assertTrue(item.isPresent());
        assertTrue(item.get().getTtl().isPresent(), "TTL should be stored");
    }

    @Test
    @Order(4)
    void trySaveNew_withRawValue_storesBinaryPayload() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_rawkey", "default");
        byte[] rawData = "binary-payload-content".getBytes();
        KvAllValues<String> vals = new KvAllValues<>("metadata", O.of(rawData), O.empty());

        kvStore.trySaveNewStringAndRaw(key, vals);

        O<KvVersionedItemAll<String>> item = kvStore.getRawVersionedAll(key);
        assertTrue(item.isPresent());
        assertEquals("metadata", item.get().getVals().getValue());
        assertTrue(item.get().getVals().getRawValue().isPresent(), "rawValue should be stored");
        assertArrayEquals(rawData, item.get().getVals().getRawValue().get());
    }

    @Test
    @Order(5)
    void trySaveNew_withArbitraryBinaryRawValue_survivesRoundTrip() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_binary", "default");
        // Non-UTF8 binary data (all byte values 0-255)
        byte[] rawData = new byte[256];
        for (int i = 0; i < 256; i++) {
            rawData[i] = (byte) i;
        }
        KvAllValues<String> vals = new KvAllValues<>("meta", O.of(rawData), O.empty());

        kvStore.trySaveNewStringAndRaw(key, vals);

        O<KvVersionedItemAll<String>> item = kvStore.getRawVersionedAll(key);
        assertTrue(item.isPresent());
        assertArrayEquals(rawData, item.get().getVals().getRawValue().get(),
                "Arbitrary binary data must survive round-trip");
    }

    // =============================================
    // getRawVersioned / getRawVersionedAll — Read
    // =============================================

    @Test
    @Order(10)
    void getRawVersioned_returnsEmptyForMissingKey() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_missing", "default");
        O<KvVersionedItem<String>> result = kvStore.getRawVersioned(key);
        assertFalse(result.isPresent());
    }

    @Test
    @Order(11)
    void getRawVersioned_returnsValueVersionAndCreated() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_readkey", "default");
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("value1", O.empty(), O.empty()));

        O<KvVersionedItem<String>> result = kvStore.getRawVersioned(key);

        assertTrue(result.isPresent());
        assertEquals("value1", result.get().getValue());
        assertEquals(1L, ((Number) result.get().getVersion()).longValue(), "Initial version should be 1");
        assertNotNull(result.get().getCreated(), "Created timestamp should be set");
    }

    @Test
    @Order(12)
    void getRawVersionedAll_includesRawValue() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_allkey", "default");
        byte[] raw = new byte[]{1, 2, 3, 4, 5};
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("val", O.of(raw), O.empty()));

        O<KvVersionedItemAll<String>> result = kvStore.getRawVersionedAll(key);

        assertTrue(result.isPresent());
        assertEquals("val", result.get().getVals().getValue());
        assertTrue(result.get().getVals().getRawValue().isPresent());
        assertArrayEquals(raw, result.get().getVals().getRawValue().get());
    }

    @Test
    @Order(13)
    void getRawVersionedAll_returnsEmptyForMissingKey() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_noall", "default");
        O<KvVersionedItemAll<String>> result = kvStore.getRawVersionedAll(key);
        assertFalse(result.isPresent());
    }

    @Test
    @Order(14)
    void getRawVersioned_withTTL_returnsTtlEpochMs() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_readttl", "default");
        ZonedDateTime ttl = times.nowZ().plusHours(1);
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("val", O.empty(), O.of(ttl)));

        O<KvVersionedItem<String>> result = kvStore.getRawVersioned(key);

        assertTrue(result.isPresent());
        assertTrue(result.get().getTtl().isPresent());
        long expectedMs = ttl.toInstant().toEpochMilli();
        long actualMs = result.get().getTtl().get().toInstant().toEpochMilli();
        assertEquals(expectedMs, actualMs, "TTL epoch ms should match exactly");
    }

    // =============================================
    // updateStringAndRaw — Update
    // =============================================

    @Test
    @Order(20)
    void update_modifiesValueAndIncrementsVersion() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_updatekey", "default");
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("original", O.empty(), O.empty()));

        OneOf<O<KvAllValues<String>>, Exception> result = kvStore.updateStringAndRaw(key,
                current -> O.of(new KvAllValues<>("updated", current.getRawValue(), current.getTtl())));

        assertTrue(result.isLeft());
        assertTrue(result.left().isPresent());
        assertEquals("updated", result.left().get().getValue());

        O<KvVersionedItem<String>> item = kvStore.getRawVersioned(key);
        assertTrue(item.isPresent());
        assertEquals(2L, ((Number) item.get().getVersion()).longValue(), "Version should be 2 after update");
    }

    @Test
    @Order(21)
    void update_createsWithDefaultIfKeyMissing() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_newupdate", "my_default");

        OneOf<O<KvAllValues<String>>, Exception> result = kvStore.updateStringAndRaw(key,
                current -> O.of(new KvAllValues<>("modified_" + current.getValue(), O.empty(), O.empty())));

        assertTrue(result.isLeft());
        assertTrue(result.left().isPresent());
        assertEquals("modified_my_default", result.left().get().getValue(),
                "Should use default 'my_default' as initial, then apply updater");
    }

    @Test
    @Order(22)
    void update_returnsEmptyWhenUpdaterReturnsEmpty() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_noop", "default");
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("val", O.empty(), O.empty()));

        OneOf<O<KvAllValues<String>>, Exception> result = kvStore.updateStringAndRaw(key, current -> O.empty());

        assertTrue(result.isLeft());
        assertFalse(result.left().isPresent(), "Empty updater result should yield empty");

        // Original value unchanged
        O<KvVersionedItem<String>> item = kvStore.getRawVersioned(key);
        assertTrue(item.isPresent());
        assertEquals("val", item.get().getValue());
    }

    @Test
    @Order(23)
    void update_canSetTTL() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_ttlupdate", "default");
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("val", O.empty(), O.empty()));

        ZonedDateTime newTtl = times.nowZ().plusMinutes(10);
        kvStore.updateStringAndRaw(key,
                current -> O.of(new KvAllValues<>(current.getValue(), O.empty(), O.of(newTtl))));

        O<KvVersionedItem<String>> item = kvStore.getRawVersioned(key);
        assertTrue(item.isPresent());
        assertTrue(item.get().getTtl().isPresent(), "TTL should be set after update");
    }

    @Test
    @Order(24)
    void update_canChangeRawValue() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_rawupdate", "default");
        byte[] initialRaw = "initial".getBytes();
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("val", O.of(initialRaw), O.empty()));

        byte[] updatedRaw = "updated-raw-data".getBytes();
        kvStore.updateStringAndRaw(key,
                current -> O.of(new KvAllValues<>(current.getValue(), O.of(updatedRaw), current.getTtl())));

        O<KvVersionedItemAll<String>> item = kvStore.getRawVersionedAll(key);
        assertTrue(item.isPresent());
        assertArrayEquals(updatedRaw, item.get().getVals().getRawValue().get());
    }

    @Test
    @Order(25)
    void update_multipleTimesIncrementsVersionCorrectly() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_multiupdate", "default");
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("v0", O.empty(), O.empty()));

        for (int i = 1; i <= 5; i++) {
            final int idx = i;
            kvStore.updateStringAndRaw(key,
                    current -> O.of(new KvAllValues<>("v" + idx, O.empty(), O.empty())));
        }

        O<KvVersionedItem<String>> item = kvStore.getRawVersioned(key);
        assertTrue(item.isPresent());
        assertEquals("v5", item.get().getValue());
        assertEquals(6L, ((Number) item.get().getVersion()).longValue(),
                "Version should be 6 after 1 create + 5 updates");
    }

    // =============================================
    // clearValue — Delete
    // =============================================

    @Test
    @Order(30)
    void clearValue_deletesExistingKey() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_clearme", "default");
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("val", O.empty(), O.empty()));

        kvStore.clearValue(key);

        O<KvVersionedItem<String>> result = kvStore.getRawVersioned(key);
        assertFalse(result.isPresent(), "Key should be gone after clearValue");
    }

    @Test
    @Order(31)
    void clearValue_isIdempotentForMissingKey() {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_nevercreated", "default");
        assertDoesNotThrow(() -> kvStore.clearValue(key));
    }

    // =============================================
    // clearAll — Flush
    // =============================================

    @Test
    @Order(40)
    void clearAll_deletesEverythingInTestProfile() {
        kvStore.trySaveNewStringAndRaw(
                new KvSimpleKeyWithName("TEST_a", "d"), new KvAllValues<>("1", O.empty(), O.empty()));
        kvStore.trySaveNewStringAndRaw(
                new KvSimpleKeyWithName("TEST_b", "d"), new KvAllValues<>("2", O.empty(), O.empty()));

        kvStore.clearAll();

        assertFalse(kvStore.getRawVersioned(new KvSimpleKeyWithName("TEST_a", "d")).isPresent());
        assertFalse(kvStore.getRawVersioned(new KvSimpleKeyWithName("TEST_b", "d")).isPresent());
    }

    // =============================================
    // Concurrency — Atomicity
    // =============================================

    @Test
    @Order(50)
    void trySaveNew_atomicUnderConcurrency_exactlyOneSucceeds() throws Exception {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_concurrent", "default");
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    OneOf<Boolean, Exception> result = kvStore.trySaveNewStringAndRaw(
                            key, new KvAllValues<>("thread-" + idx, O.empty(), O.empty()));
                    if (result.isLeft() && result.left()) {
                        successCount.incrementAndGet();
                    } else if (result.isLeft()) {
                        falseCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one thread should create the key");
        assertEquals(threadCount - 1, falseCount.get(), "All other threads should get false");
    }

    @Test
    @Order(51)
    void update_concurrentIncrementsAllApplied() throws Exception {
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("TEST_concupdate", "0");
        kvStore.trySaveNewStringAndRaw(key, new KvAllValues<>("0", O.empty(), O.empty()));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OneOf<O<KvAllValues<String>>, Exception> result = kvStore.updateStringAndRaw(key,
                            current -> {
                                int currentVal = Integer.parseInt(current.getValue());
                                return O.of(new KvAllValues<>(String.valueOf(currentVal + 1), O.empty(), O.empty()));
                            });
                    if (!result.isLeft()) {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(0, errorCount.get(), "No errors expected with CAS retry");

        O<KvVersionedItem<String>> item = kvStore.getRawVersioned(key);
        assertTrue(item.isPresent());
        assertEquals(String.valueOf(threadCount), item.get().getValue(),
                "All concurrent increments should be applied via CAS");
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
