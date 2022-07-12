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

import sk.services.nodeinfo.INodeInfo;
import sk.services.time.ITime;
import sk.utils.statics.St;
import sk.utils.statics.Ti;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.params.WebAdditionalParams;

import javax.inject.Inject;

public class WebDefaultHeadersFilter implements WebServerFilter {
    public static final int PRIORITY = WebRequestLoggingFilter.PRIORITY + PRIORITY_STEP;

    @Inject INodeInfo nodeInfo;
    @Inject ITime times;
    @Inject WebAdditionalParams conf;

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        try {
            return requestContext.getNextInChain().invokeNext();
        } finally {
            final WebRequestInnerContext ctx = requestContext.getRequestContext();
            ctx.setResponseHeader("_nId", nodeInfo.getNodeId());
            ctx.setResponseHeader("_nVer", nodeInfo.getNodeVersion());
            ctx.setResponseHeader("_rId", ctx.getServerRequestId());
            ctx.setResponseHeader("_reqStart", Ti.yyyyMMddHHmmssSSS.format(ctx.getStartTime()));
            final long rawDif = times.getDifWithNano4Dif(ctx.getReqStartNano4Dif());
            long left = rawDif / 1_000_000;
            long right = rawDif % 1_000_000;
            ctx.setResponseHeader("_rDur", left + "." + (St.ss(right + "", 0, 2)));
            conf.getCrossOrigin(ctx.getRequestHeader("Origin")).ifPresent($ -> {
                ctx.setResponseHeader("Access-Control-Allow-Origin", $);
                ctx.setResponseHeader("Access-Control-Allow-Credentials", "true");
            });
        }
    }
}
