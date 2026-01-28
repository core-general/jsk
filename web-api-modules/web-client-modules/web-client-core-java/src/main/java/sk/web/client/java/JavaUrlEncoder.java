package sk.web.client.java;

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

import sk.utils.statics.Ex;
import sk.web.client.WebUrlEncoder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Java implementation of WebUrlEncoder using java.net.URLEncoder.
 */
public class JavaUrlEncoder implements WebUrlEncoder {

    @Override
    public String encode(String value) {
        return Ex.getIgnore(() -> URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
    }
}
