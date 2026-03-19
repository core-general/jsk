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

import sk.utils.functional.O;

/**
 * Configuration interface for Redis connection.
 * Consumer projects implement this to provide environment-specific settings.
 */
public interface RedisProperties {
    String getHost();

    default int getPort() {
        return 6379;
    }

    default O<String> getPassword() {
        return O.empty();
    }

    /**
     * Key prefix for namespacing all keys in this Redis instance.
     * Example: "pindamap_prod", "pindamap_dev"
     */
    String getKeyPrefix();

    default int getPoolMaxTotal() {
        return 20;
    }

    default int getPoolMaxIdle() {
        return 10;
    }

    default int getPoolMinIdle() {
        return 2;
    }

    default long getPoolMaxWaitMillis() {
        return 2000;
    }

    default boolean getPoolTestOnBorrow() {
        return true;
    }

    default boolean getPoolTestWhileIdle() {
        return true;
    }
}
