package sk.web.client;

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

/**
 * Platform abstraction for URL encoding.
 * Java uses URLEncoder.encode, TeaVM may use alternative implementations.
 */
public interface WebUrlEncoder {
    /**
     * Encodes a string for use in a URL parameter value.
     *
     * @param value The value to encode
     * @return The URL-encoded value
     */
    String encode(String value);
}
