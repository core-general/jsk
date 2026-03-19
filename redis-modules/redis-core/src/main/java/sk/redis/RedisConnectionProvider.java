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

import redis.clients.jedis.Jedis;
import sk.utils.functional.C1;
import sk.utils.functional.F1;

import java.util.List;

/**
 * Abstraction over Redis connection pool.
 * Default implementation uses JedisPool. Can be replaced for testing or Lettuce migration.
 */
public interface RedisConnectionProvider {

    /**
     * Execute an action with a borrowed Jedis connection and return a result.
     * Connection is automatically returned to the pool after execution.
     */
    <T> T withConnection(F1<Jedis, T> action);

    /**
     * Execute an action with a borrowed Jedis connection (no return value).
     */
    void withConnectionVoid(C1<Jedis> action);

    /**
     * Execute a Lua script by SHA. Falls back to EVAL on NOSCRIPT error.
     *
     * @param sha    SHA1 of the script
     * @param script Full script bytes (for EVAL fallback)
     * @param keys   KEYS arguments
     * @param args   ARGV arguments
     * @return Script result
     */
    Object evalSha(byte[] sha, byte[] script, List<byte[]> keys, List<byte[]> args);

    /**
     * Load a Lua script into Redis and return its SHA1 as bytes.
     */
    byte[] scriptLoad(byte[] script);

    /**
     * Shutdown / close the connection pool.
     */
    void close();
}
