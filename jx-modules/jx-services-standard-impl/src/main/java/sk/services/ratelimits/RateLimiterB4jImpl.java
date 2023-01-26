package sk.services.ratelimits;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.SneakyThrows;
import sk.utils.functional.F0;

import java.time.Duration;

public class RateLimiterB4jImpl implements IRateLimiter {
    protected final Bucket bucket;

    public RateLimiterB4jImpl(int allowedRequestPerSecond) {
        this(allowedRequestPerSecond, Duration.ofSeconds(1));
    }

    public RateLimiterB4jImpl(int allowedRequestPerSecond, Duration ofDuration) {
        Bandwidth limit = Bandwidth.simple(allowedRequestPerSecond, ofDuration);
        bucket = Bucket.builder().addLimit(limit).build();
    }

    public RateLimiterB4jImpl(Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    @SneakyThrows
    public void waitUntilPossible() {
        bucket.asBlocking().consume(1);
    }

    @Override
    public <T> T produceInLimit(F0<T> producer) {
        synchronized (bucket) {
            waitUntilPossible();
            return producer.apply();
        }
    }
}
