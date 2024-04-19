package sk.services.idempotence;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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


import jakarta.inject.Inject;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.json.IJson;
import sk.services.kv.IKvUnlimitedStore;
import sk.services.kv.KvAllValues;
import sk.services.log.ILog;
import sk.services.log.ILogCategory;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.time.Duration;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static sk.utils.functional.OneOf.left;
import static sk.utils.functional.OneOf.right;

public class IIdempProviderUnlimitedKV implements IIdempProvider {
    public static final char STRING_SIGN = 'S';
    public static final char BYTEARR_SIGN = 'B';
    public static final char ZIP_SLOW_SIGN = 'Z';
    public static final ILogCategory IDEMPOTENCE_CATEGORY = () -> "IDEMPOTENCE";
    @Inject IKvUnlimitedStore kv;
    @Inject ITime times;
    @Inject IJson json;
    @Inject IBytes bytes;
    @Inject IExcept except;
    @Inject ILog log;

    @Inject Optional<IIdempParameters> config = Optional.empty();

    @Override
    public <META> IdempLockResult<META> tryLock(String key, String requestHash, TypeWrap<META> meta,
            Duration lockDuration, O<String> additionalData) {
        final boolean lockOk = kv.trySaveNewObjectAndRaw(key(key),
                new KvAllValues<>(IIdempStoredMeta.lock(requestHash, additionalData),
                        O.empty(),
                        O.of(times.nowZ().plus(lockDuration))))
                .collect($ -> $, e -> {throw new RuntimeException("idempotence_lock_failed", e);});
        if (lockOk) {
            return IdempLockResult.lockOk();
        } else {
            final KvAllValues<IIdempStoredMeta> raw = kv.getAsObjectWithRaw(key(key), IIdempStoredMeta.class);
            IIdempStoredMeta metaData = raw.getValue();
            if (!Fu.equal(metaData.requestHash, requestHash)) {
                return IdempLockResult.badParams();
            } else if (metaData.isLockSign()) {
                if (config.map($ -> $.logRetriesWhileInLock()).orElse(false)) {
                    log.logError(IDEMPOTENCE_CATEGORY, "LOCK_BAD",
                            Cc.m("me", key,
                                    "old", metaData.getAdditionalData().orElse("NONE"),
                                    "current", additionalData.orElse("NONE")));
                }
                return IdempLockResult.lockBad();
            } else {
                return IdempLockResult.cachedValue(new IdempValue<>(
                        json.from(metaData.getMeta(), meta),
                        unwrapReturnValue(metaData.getType(), raw.getRawValue().orElse(new byte[0]))
                ));
            }
        }
    }

    @Override
    public <META> void cacheValue(String key, String requestHash, IdempValue<META> valueToCache, Duration cacheDuration) {
        kv.updateObjectAndRaw(key(key), IIdempStoredMeta.class, old -> {
            old.setTtl(O.of(times.nowZ().plus(cacheDuration)));

            final char encodingType = valueToCache.getCachedValue()
                    .collect($ -> $.length() > 800 ? ZIP_SLOW_SIGN : STRING_SIGN, $ -> BYTEARR_SIGN);

            old.setValue(old.getValue().result(encodingType, requestHash, json.to(valueToCache.getMetainfo())));
            old.setRawValue(O.of(wrapReturnType(valueToCache, encodingType)));
            return O.of(old);
        }).oRight().ifPresent($ -> log.logExc(IDEMPOTENCE_CATEGORY, $, O.of("key:" + key)));
    }

    @Override
    public void unlockOrClear(String key) {
        kv.clearValue(key(key));
    }


    private IdempKey key(String key) {
        return new IdempKey(key);
    }

    private <META> byte[] wrapReturnType(IdempValue<META> valueToCache, char encodingType) {
        return encodingType == ZIP_SLOW_SIGN
                ? bytes.zipString(valueToCache.getCachedValue().left()).get()
                : encodingType == STRING_SIGN
                        ? valueToCache.getCachedValue().left().getBytes(UTF_8)
                        : valueToCache.getCachedValue().right();
    }

    private OneOf<String, byte[]> unwrapReturnValue(char encoding, byte[] rawData) {
        if (rawData.length == 0) {
            return left("");
        }
        switch (encoding) {
            case BYTEARR_SIGN:
                return right(rawData);
            case STRING_SIGN:
                return left(new String(rawData, UTF_8));
            case ZIP_SLOW_SIGN:
                return left(bytes.unZipString(rawData).get());
            default:
                return except.throwByCode("Unknown idempotence encoding:" + encoding);
        }
    }
}
