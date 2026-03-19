package sk.test.land.testcontainers.redis;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import sk.redis.JedisConnectionProvider;
import sk.redis.RedisConnectionProvider;
import sk.redis.RedisProperties;
import sk.test.land.core.JskLand;
import sk.test.land.core.mixins.JskLandEmptyStateMixin;
import sk.test.land.testcontainers.JskLandContainer;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

/**
 * Testcontainers-based Redis landscape for integration testing.
 * Uses redis:7-alpine Docker image by default.
 * <p>
 * Follows the same pattern as {@link sk.test.land.testcontainers.JskLandContainer}:
 * <ul>
 *   <li>Extends {@code JskLandContainer<GenericContainer<?>>}</li>
 *   <li>Implements {@code JskLandEmptyStateMixin} with {@code toEmptyState()} → {@code FLUSHDB}</li>
 *   <li>Port binding via constructor</li>
 *   <li>Convenience methods for client access</li>
 * </ul>
 */
@Slf4j
public class JskLandRedis extends JskLandContainer<GenericContainer<?>> implements JskLandEmptyStateMixin {

    private static final int REDIS_PORT = 6379;
    private static final String DEFAULT_IMAGE = "redis:7-alpine";

    private final String dockerImage;
    private final String keyPrefix;

    private JedisPool jedisPool;

    public JskLandRedis(int outsidePort, String keyPrefix) {
        this(outsidePort, keyPrefix, DEFAULT_IMAGE);
    }

    public JskLandRedis(int outsidePort, String keyPrefix, String dockerImage) {
        super(outsidePort);
        this.keyPrefix = keyPrefix;
        this.dockerImage = dockerImage;
    }

    @Override
    protected GenericContainer<?> createContainer(int port) {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(dockerImage))
                .withExposedPorts(REDIS_PORT);
        container.setPortBindings(Cc.l(port + ":" + REDIS_PORT));
        return container;
    }

    /**
     * Get a JedisPool connected to the testcontainer Redis.
     * Lazily initialized on first call.
     */
    public synchronized JedisPool getJedisPool() {
        if (jedisPool == null) {
            getContainer(); // ensure started
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            jedisPool = new JedisPool(config, "localhost", getOutsidePort());
        }
        return jedisPool;
    }

    /**
     * Get a {@link RedisConnectionProvider} connected to the testcontainer Redis.
     */
    public RedisConnectionProvider getConnectionProvider() {
        return new JedisConnectionProvider(getJedisPool());
    }

    /**
     * Get {@link RedisProperties} pointing to the testcontainer Redis.
     */
    public RedisProperties getRedisProperties() {
        return new RedisProperties() {
            @Override
            public String getHost() { return "localhost"; }

            @Override
            public int getPort() { return getOutsidePort(); }

            @Override
            public O<String> getPassword() { return O.empty(); }

            @Override
            public String getKeyPrefix() { return keyPrefix; }
        };
    }

    @Override
    public void toEmptyState() {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.flushDB();
        }
    }

    @Override
    protected void doShutdown() throws Exception {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
        super.doShutdown();
    }

    @Override
    public Class<? extends JskLand> getId() {
        return JskLandRedis.class;
    }
}
