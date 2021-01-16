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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Fu;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.time.Duration;

public class IIdempProviderSingleNode implements IIdempProvider {

    Cache<String, X2<String, IdempValue<?>>> localCache = Caffeine.newBuilder()
            .maximumSize(30_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Override
    public <META> IdempLockResult<META> tryLock(String key, String requestHash, TypeWrap<META> meta,
            Duration lockDuration, O<String> ignoreAdditionalData) {
        boolean[] newValue = new boolean[]{false};
        final X2<String, IdempValue<?>> cachedData = localCache.asMap().computeIfAbsent(key, (k) -> {
            newValue[0] = true;
            return X.x(requestHash, new IdempValue<META>(true, null, null));
        });
        if (newValue[0]) {
            return IdempLockResult.lockOk();
        } else if (cachedData.i2().isEmpty()) {
            return IdempLockResult.lockBad();
        } else {
            if (Fu.equal(requestHash, cachedData.i1())) {
                return (IdempLockResult<META>) IdempLockResult.cachedValue(cachedData.i2());
            } else {
                return IdempLockResult.badParams();
            }
        }
    }

    @Override
    public <META> void cacheValue(String key, String requestHash, IdempValue<META> valueToCache, Duration cacheDuration) {
        localCache.put(key, X.x(requestHash, valueToCache));
    }

    @Override
    public void unlockOrClear(String key) {
        localCache.invalidate(key);
    }
}
