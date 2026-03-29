package sk.redis;

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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sk.services.kv.keys.KvKeyRaw;
import sk.services.kv.keys.KvSimpleKeyWithName;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RedisKVStoreImpl} key format logic.
 * These tests do NOT require Docker — they only exercise the {@code toRedisKey()} method.
 */
class RedisKVStoreImplTest {

    static final String KEY_PREFIX = "test_kv";
    static RedisKVStoreImpl kvStore;

    @BeforeAll
    static void setup() {
        kvStore = new RedisKVStoreImpl();
        // Only inject the properties needed for toRedisKey()
        setField(kvStore, "properties", new RedisProperties() {
            @Override
            public String getHost() { return "localhost"; }

            @Override
            public String getKeyPrefix() { return KEY_PREFIX; }
        });
        // Do NOT call init() — that requires Redis connection.
        // toRedisKey() only needs 'properties'.
    }

    // ========== Key Format Tests ==========

    @Test
    void keyFormat_singleCategory() {
        KvKeyRaw key = new KvKeyRaw(Cc.l("SINGLE"));
        byte[] redisKey = kvStore.toRedisKey(key);
        assertEquals(KEY_PREFIX + ":SINGLE", new String(redisKey));
    }

    @Test
    void keyFormat_twoCategories() {
        KvKeyRaw key = new KvKeyRaw(Cc.l("IDEMPOTENCE", "abc123"));
        byte[] redisKey = kvStore.toRedisKey(key);
        assertEquals(KEY_PREFIX + ":IDEMPOTENCE:abc123", new String(redisKey));
    }

    @Test
    void keyFormat_threeCategories() {
        KvKeyRaw key = new KvKeyRaw(Cc.l("NS", "part", "sort"));
        byte[] redisKey = kvStore.toRedisKey(key);
        assertEquals(KEY_PREFIX + ":NS:part:sort", new String(redisKey));
    }

    @Test
    void keyFormat_kvSimpleKeyWithName_splitsOnUnderscore() {
        // KvSimpleKeyWithName("FOO_bar", ...) → categories=["FOO","bar"] → prefix:FOO:bar
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("FOO_bar", "default");
        byte[] redisKey = kvStore.toRedisKey(key);
        assertEquals(KEY_PREFIX + ":FOO:bar", new String(redisKey));
    }

    @Test
    void keyFormat_kvSimpleKeyWithName_threePartName() {
        // KvSimpleKeyWithName("NS_part_sort", ...) → categories=["NS","part","sort"]
        KvSimpleKeyWithName key = new KvSimpleKeyWithName("NS_part_sort", "default");
        byte[] redisKey = kvStore.toRedisKey(key);
        assertEquals(KEY_PREFIX + ":NS:part:sort", new String(redisKey));
    }

    @Test
    void keyFormat_emptyCategories_throws() {
        KvKeyRaw key = new KvKeyRaw(Cc.l());
        assertThrows(IllegalArgumentException.class, () -> kvStore.toRedisKey(key));
    }

    // ========== Helpers ==========

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
