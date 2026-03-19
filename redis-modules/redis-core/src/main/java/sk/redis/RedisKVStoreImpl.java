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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sk.exceptions.NotImplementedException;
import sk.services.kv.*;
import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.kv.keys.KvLockOrRenewKey;
import sk.services.profile.IAppProfile;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class RedisKVStoreImpl extends IKvStoreJsonBased implements IKvUnlimitedStore {

    @Inject RedisConnectionProvider redis;
    @Inject RedisProperties properties;
    @Inject ITime times;
    @Inject IAppProfile<?> appProfile;

    // Lua script bytes + cached SHAs
    private byte[] trySaveNewScript;
    private byte[] trySaveNewSha;
    private byte[] compareAndSetScript;
    private byte[] compareAndSetSha;

    // Field name constants (as byte arrays)
    private static final byte[] F_V = "v".getBytes(UTF_8);
    private static final byte[] F_RV = "rv".getBytes(UTF_8);
    private static final byte[] F_VER = "ver".getBytes(UTF_8);
    private static final byte[] F_CAT = "cat".getBytes(UTF_8);
    private static final byte[] F_UAT = "uat".getBytes(UTF_8);
    private static final byte[] F_TTL = "ttl".getBytes(UTF_8);

    private static final byte[] EMPTY_BYTES = new byte[0];

    @PostConstruct
    public void init() {
        trySaveNewScript = loadLuaScript("sk/redis/lua/try_save_new.lua");
        trySaveNewSha = redis.scriptLoad(trySaveNewScript);

        compareAndSetScript = loadLuaScript("sk/redis/lua/compare_and_set.lua");
        compareAndSetSha = redis.scriptLoad(compareAndSetScript);

        log.info("RedisKVStoreImpl initialized with key prefix: {}", properties.getKeyPrefix());
    }

    // ========== 8 Abstract Method Implementations ==========

    @Override
    public O<KvVersionedItem<String>> getRawVersioned(KvKeyWithDefault key) {
        try {
            return redis.withConnection(jedis -> {
                byte[] redisKey = toRedisKey(key);
                List<byte[]> fields = jedis.hmget(redisKey, F_V, F_VER, F_CAT, F_TTL);
                // fields: [v, ver, cat, ttl] — null if field doesn't exist

                byte[] vBytes = fields.get(0);
                if (vBytes == null) {
                    return O.empty(); // key doesn't exist
                }

                String value = new String(vBytes, UTF_8);
                Object version = parseVersion(fields.get(1));
                ZonedDateTime created = parseEpochMs(fields.get(2));
                O<ZonedDateTime> ttl = O.ofNull(fields.get(3)).map(this::parseEpochMs);

                return O.of(new KvVersionedItem<>(key, value, ttl, created, version));
            });
        } catch (Exception e) {
            log.error("getRawVersioned failed for key: {}", key.categories(), e);
            return O.empty();
        }
    }

    @Override
    public O<KvVersionedItemAll<String>> getRawVersionedAll(KvKeyWithDefault key) {
        try {
            return redis.withConnection(jedis -> {
                byte[] redisKey = toRedisKey(key);
                Map<byte[], byte[]> hash = jedis.hgetAll(redisKey);

                if (hash == null || hash.isEmpty()) {
                    return O.empty();
                }

                String value = getStringField(hash, F_V);
                if (value == null) {
                    return O.empty();
                }

                byte[] rawValue = getBytesField(hash, F_RV);
                Object version = parseVersion(getBytesField(hash, F_VER));
                ZonedDateTime created = parseEpochMs(getBytesField(hash, F_CAT));
                O<ZonedDateTime> ttl = O.ofNull(getBytesField(hash, F_TTL)).map(this::parseEpochMs);

                KvAllValues<String> vals = new KvAllValues<>(
                        value,
                        rawValue != null && rawValue.length > 0 ? O.of(rawValue) : O.empty(),
                        ttl
                );

                return O.of(new KvVersionedItemAll<>(key, vals, ttl, created, version));
            });
        } catch (Exception e) {
            log.error("getRawVersionedAll failed for key: {}", key.categories(), e);
            return O.empty();
        }
    }

    @Override
    public List<KvListItemAll<String>> getRawVersionedListBetweenCategories(
            KvKey baseKey, O<String> fromLastCategory, O<String> toLastCategory,
            int maxCount, boolean ascending) {
        throw new NotImplementedException("Redis range queries not yet implemented");
    }

    @Override
    public OneOf<Boolean, Exception> trySaveNewStringAndRaw(KvKey key, KvAllValues<String> newValueProvider) {
        try {
            byte[] redisKey = toRedisKey(key);
            String nowMs = String.valueOf(times.now());
            String ttlMs = newValueProvider.getTtl()
                    .map(t -> String.valueOf(t.toInstant().toEpochMilli()))
                    .orElse("");

            List<byte[]> keys = Cc.l(redisKey);
            List<byte[]> args = Cc.l(
                    toBytes(newValueProvider.getValue()),                              // ARGV[1] = value
                    newValueProvider.getRawValue().orElse(EMPTY_BYTES),                // ARGV[2] = rawValue
                    toBytes(nowMs),                                                    // ARGV[3] = createdAt
                    toBytes(ttlMs)                                                     // ARGV[4] = TTL
            );

            Object result = redis.evalSha(trySaveNewSha, trySaveNewScript, keys, args);
            long resultLong = (Long) result;

            return OneOf.left(resultLong == 1L);
        } catch (Exception e) {
            log.error("trySaveNewStringAndRaw failed for key: {}", key.categories(), e);
            return OneOf.right(e);
        }
    }

    @Override
    public OneOf<O<KvAllValues<String>>, Exception> updateStringAndRaw(
            KvKeyWithDefault key, F1<KvAllValues<String>, O<KvAllValues<String>>> updater) {
        try {
            // Step 1: getOrCreateNewAllValues — same pattern as DynKVStoreImpl
            O<KvVersionedItemAll<String>> existing = getOrCreateAllValues(key);
            if (existing.isEmpty()) {
                return OneOf.right(new RuntimeException("Failed to create default value for key: " + key.categories()));
            }

            // Step 2: Retry loop with CAS (matches DynamoDB pattern: 1000 retries, 200ms sleep)
            int retries = 1000;
            while (retries-- > 0) {
                KvVersionedItemAll<String> current = existing.get();
                KvAllValues<String> currentVals = current.getVals();

                // Apply updater
                O<KvAllValues<String>> updatedOpt = updater.apply(currentVals);
                if (updatedOpt.isEmpty()) {
                    return OneOf.left(O.empty());
                }

                KvAllValues<String> updated = updatedOpt.get();
                long currentVersion = ((Number) current.getVersion()).longValue();
                String versionStr = String.valueOf(currentVersion);
                String nowMs = String.valueOf(times.now());
                String ttlMs = updated.getTtl()
                        .map(t -> String.valueOf(t.toInstant().toEpochMilli()))
                        .orElse("");

                byte[] redisKey = toRedisKey(key);
                List<byte[]> keys = Cc.l(redisKey);
                List<byte[]> args = Cc.l(
                        toBytes(versionStr),                              // ARGV[1] = expected version
                        toBytes(updated.getValue()),                      // ARGV[2] = new value
                        updated.getRawValue().orElse(EMPTY_BYTES),        // ARGV[3] = new rawValue
                        toBytes(nowMs),                                   // ARGV[4] = updatedAt
                        toBytes(ttlMs)                                    // ARGV[5] = new TTL
                );

                Object result = redis.evalSha(compareAndSetSha, compareAndSetScript, keys, args);
                long resultLong = (Long) result;

                if (resultLong == 1L) {
                    return OneOf.left(O.of(updated));
                } else if (resultLong == 0L) {
                    // Version mismatch — retry
                    try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    existing = getOrCreateAllValues(key);
                    if (existing.isEmpty()) {
                        return OneOf.right(
                                new RuntimeException("Key vanished and could not be re-created: " + key.categories()));
                    }
                    continue;
                } else {
                    // resultLong == -1: key not found — re-create and retry
                    existing = getOrCreateAllValues(key);
                    if (existing.isEmpty()) {
                        return OneOf.right(
                                new RuntimeException("Key vanished and could not be re-created: " + key.categories()));
                    }
                    continue;
                }
            }

            return OneOf.right(new RuntimeException("CAS retry exhausted (1000 attempts) for key: " + key.categories()));
        } catch (Exception e) {
            log.error("updateStringAndRaw failed for key: {}", key.categories(), e);
            return OneOf.right(e);
        }
    }

    @Override
    public boolean tryLockOrRenew(KvLockOrRenewKey key, String whoLocks, O<Long> lockIsOldAfterOrElseRenew) {
        throw new NotImplementedException("Redis distributed locks not yet implemented");
    }

    @Override
    public void clearValue(KvKey key) {
        int retries = 10;
        while (retries-- > 0) {
            try {
                redis.withConnectionVoid(jedis -> jedis.del(toRedisKey(key)));
                return;
            } catch (Exception e) {
                if (retries <= 0) {
                    throw new RuntimeException("clearValue failed after retries for key: " + key.categories(), e);
                }
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    @Override
    public void clearAll() {
        if (appProfile.getProfile().isForProductionUsage()) {
            throw new RuntimeException("clearAll is forbidden in production");
        }
        redis.withConnectionVoid(jedis -> jedis.flushDB());
    }

    // ========== Helpers ==========

    /**
     * Replicates DynKVStoreImpl.getOrCreateNewAllValues pattern:
     * 1. Try getRawVersionedAll
     * 2. If empty → trySaveNewStringAndRaw with default, then re-read
     */
    private O<KvVersionedItemAll<String>> getOrCreateAllValues(KvKeyWithDefault key) {
        O<KvVersionedItemAll<String>> existing = getRawVersionedAll(key);
        if (existing.isEmpty()) {
            trySaveNewStringAndRaw(key, new KvAllValues<>(key.getDefaultValue(), O.empty(), O.empty()));
            existing = getRawVersionedAll(key);
        }
        return existing;
    }

    /**
     * Build Redis key from KvKey categories.
     * Format: "{prefix}:{cat[0]}:{cat[1]}:{...}"
     */
    byte[] toRedisKey(KvKey key) {
        List<String> categories = key.categories();
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("KvKey provides no categories: " + key);
        }
        return (properties.getKeyPrefix() + ":" + String.join(":", categories)).getBytes(UTF_8);
    }

    private byte[] toBytes(String s) {
        return s == null ? EMPTY_BYTES : s.getBytes(UTF_8);
    }

    private String getStringField(Map<byte[], byte[]> hash, byte[] fieldName) {
        for (Map.Entry<byte[], byte[]> entry : hash.entrySet()) {
            if (Arrays.equals(entry.getKey(), fieldName)) {
                return entry.getValue() != null ? new String(entry.getValue(), UTF_8) : null;
            }
        }
        return null;
    }

    private byte[] getBytesField(Map<byte[], byte[]> hash, byte[] fieldName) {
        for (Map.Entry<byte[], byte[]> entry : hash.entrySet()) {
            if (Arrays.equals(entry.getKey(), fieldName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Object parseVersion(byte[] bytes) {
        if (bytes == null) return 0L;
        return Long.parseLong(new String(bytes, UTF_8));
    }

    private ZonedDateTime parseEpochMs(byte[] bytes) {
        if (bytes == null) return null;
        long ms = Long.parseLong(new String(bytes, UTF_8));
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC);
    }

    private byte[] loadLuaScript(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Lua script not found on classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), UTF_8).getBytes(UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Lua script: " + resourcePath, e);
        }
    }
}
