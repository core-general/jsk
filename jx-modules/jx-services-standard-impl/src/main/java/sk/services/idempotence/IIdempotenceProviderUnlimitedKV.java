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

import org.jetbrains.annotations.NotNull;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.json.IJson;
import sk.services.kv.IKvUnlimitedStore;
import sk.services.kv.KvAllValues;
import sk.services.log.ILog;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static sk.utils.functional.OneOf.left;
import static sk.utils.functional.OneOf.right;

public class IIdempotenceProviderUnlimitedKV implements IIdempotenceProvider {
    public static final char STRING_SIGN = 'S';
    public static final char BYTEARR_SIGN = 'B';
    public static final char ZIP_SLOW_SIGN = 'Z';
    @Inject IKvUnlimitedStore kv;
    @Inject ITime times;
    @Inject IJson json;
    @Inject IBytes bytes;
    @Inject IExcept except;
    @Inject ILog log;

    @Inject Optional<IIdempotenceParameters> config = Optional.empty();

    @Override
    public <META> IdempotenceLockResult<META> tryLock(String key, String requestHash, TypeWrap<META> meta,
            Duration lockDuration, O<String> additionalData) {
        final boolean lockOk = kv.trySaveNewObjectAndRaw(key(key),
                new KvAllValues<>(IIdempotenceStoredMeta.lock(requestHash, times.nowZ(), additionalData),
                        O.empty(),
                        O.of(times.nowZ().plus(lockDuration))))
                .collect($ -> $, e -> {throw new RuntimeException("idempotence_lock_failed", e);});
        if (lockOk) {
            return IdempotenceLockResult.lockOk();
        } else {
            final KvAllValues<IIdempotenceStoredMeta> raw = kv.getAsObjectWithRaw(key(key), IIdempotenceStoredMeta.class);
            IIdempotenceStoredMeta metaData = raw.getValue();
            if (!Fu.equal(metaData.requestHash, requestHash)) {
                return IdempotenceLockResult.badParams();
            } else if (metaData.isLockSign()) {
                if (config.map($ -> $.logRetriesWhileInLock()).orElse(false)) {
                    log.logError(() -> "IDEMPOTENCE", "LOCK_BAD", Cc.m("me", key, "meta", json.to(metaData)));
                }
                return IdempotenceLockResult.lockBad();
            } else {
                return IdempotenceLockResult.cachedValue(new IdempotentValue<>(
                        json.from(metaData.getMeta(), meta),
                        unwrapReturnValue(metaData.getType(), raw.getRawValue().orElse(new byte[0]))
                ));
            }
        }
    }

    @Override
    public <META> void cacheValue(String key, String requestHash, IdempotentValue<META> valueToCache, Duration cacheDuration) {
        kv.updateObjectAndRaw(key(key), IIdempotenceStoredMeta.class, old -> {
            old.setTtl(O.of(times.nowZ().plus(cacheDuration)));

            final char encodingType = valueToCache.getCachedValue()
                    .collect($ -> $.length() > 800 ? ZIP_SLOW_SIGN : STRING_SIGN, $ -> BYTEARR_SIGN);

            old.setValue(IIdempotenceStoredMeta.result(encodingType, requestHash, json.to(valueToCache.getMetainfo())));
            old.setRawValue(O.of(wrapReturnType(valueToCache, encodingType)));
            return O.of(old);
        });
    }

    @Override
    public void unlockOrClear(String key) {
        kv.clearValue(key(key));
    }

    @NotNull
    private IdempotenceKey key(String key) {
        return new IdempotenceKey(key);
    }

    private <META> byte[] wrapReturnType(IdempotentValue<META> valueToCache, char encodingType) {
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
