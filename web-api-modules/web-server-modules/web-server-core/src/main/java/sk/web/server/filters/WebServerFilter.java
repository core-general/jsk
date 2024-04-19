package sk.web.server.filters;

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


import sk.web.renders.WebFilterOutput;

/**
 * Stateless filters, they must be
 */
public interface WebServerFilter extends Comparable<WebServerFilter> {
    public static final int PRIORITY_STEP = 1_000_000;

    public int getFilterPriority();

    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext);

    @Override
    default int compareTo(WebServerFilter o) {
        return getFilterPriority() - o.getFilterPriority();
    }
}
