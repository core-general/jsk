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

import lombok.extern.log4j.Log4j2;
import sk.exceptions.JskProblem;
import sk.utils.functional.O;
import sk.web.exceptions.IWebExcept;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestResponseInfo;
import sk.web.server.context.WebRequestVariable;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;

import javax.inject.Inject;

@Log4j2
public class WebRequestLoggingFilter implements WebServerFilter {
    public static final WebRequestVariable WEB_FORCE_LOG = () -> "_JSK_FORCE_REQUEST_LOG";
    public static final int PRIORITY = 0;

    @Inject IWebExcept webExcept;
    @Inject WebRequestResponseInfo reqRespInfo;

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        O<WebFilterOutput> oWr = O.empty();
        try {
            if (requestContext.allowAuxiliaryFunction(WEB_FORCE_LOG)) {
                log.debug(() -> reqRespInfo.getRequestInfo(requestContext));
            }
            WebFilterOutput webReply = requestContext.getNextInChain().invokeNext();
            oWr = O.of(WebFilterOutput.rendered(
                    requestContext.getRequestContext().getWebRender()
                            .getResult(webReply, webExcept.getDefaultExceptionRender())));
            return oWr.get();
        } catch (Exception e) {
            oWr = O.of(WebFilterOutput
                    .rawProblem(0, JskProblem.description("Exception handled in log filter:" + e.getMessage())));
            throw e;
        } finally {
            if (requestContext.allowAuxiliaryFunction(WEB_FORCE_LOG)) {
                O<WebFilterOutput> finalOWr = oWr;
                log.debug(() -> reqRespInfo.getResponseInfo(requestContext, finalOWr));
            }
        }
    }
}
