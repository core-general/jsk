package sk.redis.spring;

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.redis.RedisConnectionProvider;
import sk.redis.RedisProperties;

/**
 * Abstract Spring config providing Redis connection.
 * Consumer projects extend this and implement RedisProperties bean.
 *
 * <p>The RedisConnectionProvider bean is abstract now and will be made concrete
 * in P1B when JedisConnectionProvider implementation is available.</p>
 */
@Configuration
public abstract class RedisBeanConfig {

    @Bean
    public abstract RedisProperties RedisProperties();

    /** Will return new JedisConnectionProvider(properties) after P1B */
    @Bean
    public abstract RedisConnectionProvider RedisConnectionProvider(RedisProperties properties);
}
