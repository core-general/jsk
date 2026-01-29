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

import sk.web.infogatherer.WebClassInfo;

/**
 * Platform abstraction for creating API client instances.
 * Java uses dynamic proxies, TeaVM uses compile-time generated classes.
 */
public interface WebPlatformSpecificHelper {
    /**
     * Creates a client instance for the given API interface.
     *
     * @param apiCls        The API interface class
     * @param classInfo     Pre-computed method/class metadata
     * @param methodInvoker Callback to execute when a method is called
     * @param <API>         The API interface type
     * @return The API client instance
     */
    <API> API createClient(
            Class<API> apiCls,
            WebClassInfo classInfo,
            WebMethodInvokeHandler methodInvoker
    );

    /**
     * Encodes a string for use in a URL parameter value.
     *
     * @param value The value to urlEncode
     * @return The URL-encoded value
     */
    String urlEncode(String value);
}
