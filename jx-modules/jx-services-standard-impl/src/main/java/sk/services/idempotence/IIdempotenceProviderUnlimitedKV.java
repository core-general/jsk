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
import sk.services.kv.KvVersionedItemAll;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;

import javax.inject.Inject;
import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IIdempotenceProviderUnlimitedKV implements IIdempotenceProvider {
    public static final String LOCK_SIGN = "*";
    public static final char STRING_SIGN = 'S';
    public static final char BYTEARR_SIGN = 'B';
    public static final char ZIP_SLOW_SIGN = 'Z';
    public static final char ZIP_FAST_SIGN = 'F';
    @Inject IKvUnlimitedStore kv;
    @Inject ITime times;
    @Inject IJson json;
    @Inject IBytes bytes;
    @Inject IExcept except;

    @Override
    public <META> IdempotenceLockResult<META> tryLock(String key, TypeWrap<META> meta, Duration lockDuration) {
        final boolean lockOk = kv.trySaveNewStringAndRaw(key(key),
                new KvAllValues<>(LOCK_SIGN, O.empty(), O.of(times.nowZ().plus(lockDuration))))
                .collect($ -> $, e -> false);
        if (lockOk) {
            return new IdempotenceLockResult<>(OneOf.right(true));
        } else {
            final O<KvVersionedItemAll<String>> oRes = kv.getRawVersionedAll(key(key));
            if (oRes.isEmpty()) {
                return tryLock(key, meta, lockDuration);
            }
            final KvVersionedItemAll<String> res = oRes.get();
            final String metaInfo = res.getVals().getValue();
            if (LOCK_SIGN.equalsIgnoreCase(metaInfo)) {
                return new IdempotenceLockResult<>(OneOf.right(false));
            } else {
                final char encodedType = metaInfo.charAt(0);
                return new IdempotenceLockResult<>(OneOf.left(
                        new IdempotentValue<>(json.from(metaInfo.substring(2), meta),
                                unwrapReturnValue(encodedType, res.getVals().getRawValue().orElse(new byte[0])))
                ));
            }
        }
    }

    @Override
    public <META> void cacheValue(String key, IdempotentValue<META> valueToCache, Duration cacheDuration) {
        kv.updateStringAndRaw(key(key), old -> {
            old.setTtl(O.of(times.nowZ().plus(cacheDuration)));
            final byte[] cachedValue = valueToCache.getCachedValue()
                    .collect($ -> $.length() > 800 ? bytes.zipString($).get() : $.getBytes(UTF_8), $ -> $);
            old.setValue(valueToCache.getCachedValue()
                    .collect($ -> $.length() > 800 ? ZIP_SLOW_SIGN : STRING_SIGN, $ -> BYTEARR_SIGN) + "_" +
                    json.to(valueToCache.getMetainfo()));
            old.setRawValue(O.of(cachedValue));
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

    private OneOf<String, byte[]> unwrapReturnValue(char encoding, byte[] rawData) {
        if (rawData.length == 0) {
            return OneOf.left("");
        }
        switch (encoding) {
            case BYTEARR_SIGN:
                return OneOf.right(rawData);
            case STRING_SIGN:
                return OneOf.left(new String(rawData, UTF_8));
            case ZIP_SLOW_SIGN:
                return OneOf.left(bytes.unZipString(rawData).get());
            default:
                return except.throwByCode("Unknown idempotence encoding:" + encoding);
        }
    }
}
