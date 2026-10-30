package sk.spring.errors;

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

import java.util.List;

public record ErrorEvent(
        String id,
        String service,
        String environment,
        String time,
        String kind,
        String type,
        String message,
        List<String> frames,
        String logger,
        String site,
        String template,
        String trace) {
    public static final String EXCEPTION = "exception";
    public static final String LOG = "log";
    public static final String PROBLEM = "problem";
}
