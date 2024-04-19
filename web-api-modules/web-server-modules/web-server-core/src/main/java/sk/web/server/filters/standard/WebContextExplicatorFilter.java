package sk.web.server.filters.standard;

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

import jakarta.inject.Inject;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebContextHolder;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.filters.additional.WebUserActionLoggingFilter;

public class WebContextExplicatorFilter implements WebServerFilter {
    public static final int PRIORITY = WebUserActionLoggingFilter.PRIORITY - PRIORITY_STEP;

    @Inject WebContextHolder ctxHolder;

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        ctxHolder.set(requestContext.getRequestContext());
        try {
            return requestContext.getNextInChain().invokeNext();
        } finally {
            ctxHolder.del();
        }
    }

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }
}
