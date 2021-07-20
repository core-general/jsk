package sk.services.ipgeo;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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
import com.maxmind.db.CacheKey;
import com.maxmind.db.DecodedValue;
import com.maxmind.db.NodeCache;

import java.io.IOException;

public class IpGeoCache implements NodeCache {
    final Cache<CacheKey<?>, DecodedValue> cache;

    public IpGeoCache() {
        this(4096);
    }

    public IpGeoCache(int cacheSize) {
        cache = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .build();
    }

    @Override
    public DecodedValue get(CacheKey cacheKey, Loader loader) throws IOException {
        try {
            DecodedValue cachedValue = cache.getIfPresent(cacheKey);
            if (cachedValue == null) {
                final DecodedValue value = loader.load(cacheKey);
                cachedValue = cache.get(cacheKey, k -> value);
            }
            return cachedValue;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
