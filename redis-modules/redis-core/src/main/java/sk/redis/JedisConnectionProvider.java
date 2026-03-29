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

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisNoScriptException;
import sk.utils.functional.C1;
import sk.utils.functional.F1;

import java.util.List;

/**
 * JedisPool-backed implementation of RedisConnectionProvider.
 * Each operation borrows a connection from the pool, executes, and returns it.
 */
@Slf4j
public class JedisConnectionProvider implements RedisConnectionProvider {
    private final JedisPool pool;

    public JedisConnectionProvider(RedisProperties properties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(properties.getPoolMaxTotal());
        poolConfig.setMaxIdle(properties.getPoolMaxIdle());
        poolConfig.setMinIdle(properties.getPoolMinIdle());
        poolConfig.setMaxWaitMillis(properties.getPoolMaxWaitMillis());
        poolConfig.setTestOnBorrow(properties.getPoolTestOnBorrow());
        poolConfig.setTestWhileIdle(properties.getPoolTestWhileIdle());

        this.pool = properties.getPassword()
                .map(pw -> new JedisPool(poolConfig, properties.getHost(), properties.getPort(),
                        (int) properties.getPoolMaxWaitMillis(), pw))
                .orElseGet(() -> new JedisPool(poolConfig, properties.getHost(), properties.getPort(),
                        (int) properties.getPoolMaxWaitMillis()));

        log.info("JedisConnectionProvider initialized: {}:{}, pool maxTotal={}",
                properties.getHost(), properties.getPort(), properties.getPoolMaxTotal());
    }

    /**
     * Constructor for testing — accepts a pre-built JedisPool.
     */
    public JedisConnectionProvider(JedisPool pool) {
        this.pool = pool;
    }

    @Override
    public <T> T withConnection(F1<Jedis, T> action) {
        try (Jedis jedis = pool.getResource()) {
            return action.apply(jedis);
        }
    }

    @Override
    public void withConnectionVoid(C1<Jedis> action) {
        try (Jedis jedis = pool.getResource()) {
            action.accept(jedis);
        }
    }

    @Override
    public Object evalSha(byte[] sha, byte[] script, List<byte[]> keys, List<byte[]> args) {
        try (Jedis jedis = pool.getResource()) {
            try {
                return jedis.evalsha(sha, keys, args);
            } catch (JedisNoScriptException e) {
                // Script not cached on this Redis server — fall back to EVAL
                log.debug("Lua script SHA not found, falling back to EVAL");
                return jedis.eval(script, keys, args);
            }
        }
    }

    @Override
    public byte[] scriptLoad(byte[] script) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.scriptLoad(script);
        }
    }

    @Override
    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
            log.info("JedisConnectionProvider closed");
        }
    }
}
