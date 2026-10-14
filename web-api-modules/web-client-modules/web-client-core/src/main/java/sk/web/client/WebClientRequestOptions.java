package sk.web.client;

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

import java.time.Duration;

public record WebClientRequestOptions(boolean followRedirects, Duration totalTimeout, long maxResponseBytes, Duration connectTimeout) {
    public static final WebClientRequestOptions DEFAULT = new WebClientRequestOptions(true, null, Long.MAX_VALUE);

    public WebClientRequestOptions(boolean followRedirects, Duration totalTimeout, long maxResponseBytes) {
        this(followRedirects, totalTimeout, maxResponseBytes, null);
    }

    public WebClientRequestOptions {
        if (totalTimeout != null && (totalTimeout.isNegative() || totalTimeout.isZero()))
            throw new IllegalArgumentException("Total HTTP timeout must be positive");
        if (maxResponseBytes < 1) throw new IllegalArgumentException("Response limit must be positive");
        if (connectTimeout != null && (connectTimeout.isNegative() || connectTimeout.isZero()))
            throw new IllegalArgumentException("HTTP connect timeout must be positive");
    }
}
