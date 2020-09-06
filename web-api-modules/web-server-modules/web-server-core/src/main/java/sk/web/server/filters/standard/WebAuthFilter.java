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

import sk.exceptions.JskProblem;
import sk.services.bean.IServiceProvider;
import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.St;
import sk.web.annotations.WebAuth;
import sk.web.auth.WebAuthServer;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;

import javax.inject.Inject;

public class WebAuthFilter implements WebServerFilter {
    public static final int PRIORITY = WebShutdownFilter.PRIORITY + PRIORITY_STEP;

    @Inject IServiceProvider services;

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        final O<WebAuth> oAuth = requestContext.getRequestContext().getWebAuth();
        if (oAuth.isPresent()) {
            final WebAuth auth = oAuth.get();
            final WebRequestInnerContext ctx = requestContext.getRequestContext();

            O<String> secretValue = auth.isParamOrHeader()
                    ? ctx.getParamAsString(auth.paramName())
                    : ctx.getRequestHeader(auth.paramName());

            boolean result;
            if (!St.isNullOrEmpty(auth.getPassword())) {
                result = Fu.equal(secretValue.orElse(null), auth.getPassword());
            } else {
                final WebAuthServer prov = services.getService(auth.srvProvider()).get();
                result = prov.authenticate(secretValue);
            }

            if (!result) {
                return WebFilterOutput.rawProblem(401, JskProblem.substatus("auth_failed",
                        "Auth failed for " + (auth.isParamOrHeader() ? "param" : "header") + ":" + auth.paramName()));
            }
        }
        return requestContext.getNextInChain().invokeNext();
    }

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }
}
