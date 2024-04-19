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
import sk.web.exceptions.IWebExcept;
import sk.web.renders.WebFilterOutput;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;

public class WebRenderFilter implements WebServerFilter {
    public static final int PRIORITY = WebAuthFilter.PRIORITY + PRIORITY_STEP;

    @Inject IWebExcept except;

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        final WebFilterOutput reply = requestContext.getNextInChain().invokeNext();
        return WebFilterOutput
                .rendered(reply.render(requestContext.getRequestContext().getWebRender(), except, requestContext.getApiMethod()));
    }
}
