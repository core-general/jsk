package sk.web.server.context;

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
import sk.services.free.IFree;
import sk.services.json.IJson;
import sk.services.time.ITime;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.statics.Ti;
import sk.web.renders.WebFilterOutput;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.model.WebRequestFinishInfo;
import sk.web.server.model.WebRequestStartInfo;

public class WebRequestResponseInfoImpl implements WebRequestResponseInfo {
    private static final WebRequestVariable WEB_REQUEST_INFO = () -> "_JSK_WEB_REQUEST_INFO";
    private static final WebRequestVariable WEB_RESPONSE_INFO = () -> "_JSK_WEB_RESPONSE_INFO";
    private static final WebRequestVariable WEB_REQUEST_INFO_RAW = () -> "_JSK_WEB_REQUEST_INFO_RAW";
    private static final WebRequestVariable WEB_RESPONSE_INFO_RAW = () -> "_JSK_WEB_RESPONSE_INFO_RAW";
    private static final int STRING_LIMIT = 50000;

    @Inject IFree free;
    @Inject ITime times;
    @Inject IJson json;

    @Override
    public <API> WebRequestStartInfo getRequestRawInfo(WebServerFilterContext<API> ctx) {
        return getOrGenerate(ctx, WEB_REQUEST_INFO_RAW, () -> generateRawRequest(ctx));
    }

    @Override
    public <API> WebRequestFinishInfo getResponseRawInfo(WebServerFilterContext<API> ctx, O<WebFilterOutput> output) {
        return getOrGenerate(ctx, WEB_RESPONSE_INFO_RAW, () -> generateRawResponse(ctx, output));
    }

    @Override
    public <API> String getRequestInfo(WebServerFilterContext<API> ctx) {
        return getOrGenerate(ctx, WEB_REQUEST_INFO, () -> generateRequestString(getRequestRawInfo(ctx)));
    }

    @Override
    public <API> String getResponseInfo(WebServerFilterContext<API> ctx, O<WebFilterOutput> output) {
        return getOrGenerate(ctx, WEB_RESPONSE_INFO, () -> generateResponseString(getResponseRawInfo(ctx, output)));
    }

    @Override
    public String generateRequestString(WebRequestStartInfo info) {
        return "\n" + free.process("sk/web/server/templates/request_start.ftl", Cc.m("info", info)) + "\n";
    }

    @Override
    public String generateResponseString(WebRequestFinishInfo info) {
        return "\n" + free.process("sk/web/server/templates/request_finish.ftl", Cc.m("info", info)) + "\n";
    }

    protected int getStringLimit() {
        return STRING_LIMIT;
    }

    private <API, T> T getOrGenerate(WebServerFilterContext<API> ctx, WebRequestVariable variable, F0<T> generator) {
        final O<T> variableValue = ctx.getRequestContext().getVariableValue(variable);
        return variableValue.orElseGet(() -> {
            T value = generator.apply();
            ctx.getRequestContext().setVariableValue(variable, value);
            return value;
        });
    }

    private <API> WebRequestStartInfo generateRawRequest(WebServerFilterContext<API> requestContext) {
        final WebRequestInnerContext ctx = requestContext.getRequestContext();
        return new WebRequestStartInfo(
                ctx.getStartTime().format(Ti.yyyyMMddHHmmssSSS),
                ctx.getServerRequestId(),
                ctx.getIpInfo().getClientIp(),
                ctx.getUserToken(),
                ctx.getUrlPathPart(),
                ctx.getApiMethodType().name(),
                ctx.getRequestHeaderNames().stream()
                        .map($ -> $ + ":" + St.raze3dots(ctx.getRequestHeader($).orElse(""), getStringLimit()))
                        .collect(Cc.toL()),
                ctx.getNonMultipartParamInfo().entrySet().stream()
                        .map($ -> $.getKey() + ":" + St.raze3dots($.getValue(), getStringLimit()))
                        .collect(Cc.toL()),
                ctx.getMultipartParamInfo().map($ -> Cc.join(", ", $))
        );
    }

    private <API> WebRequestFinishInfo generateRawResponse(WebServerFilterContext<API> requestContext,
            O<WebFilterOutput> output) {
        final WebRequestInnerContext ctx = requestContext.getRequestContext();
        final long difWithNano4Dif = times.getDifWithNano4Dif(ctx.getReqStartNano4Dif());
        String ms = difWithNano4Dif / 1_000_000 + "";
        String msRight = St.ss(difWithNano4Dif % 1_000_000 + "", 0, 2);
        return new WebRequestFinishInfo(
                ctx.getResponseHeaderNames().stream()
                        .map($ -> $ + ":" + St.raze3dots(ctx.getResponseHeader($).orElse(""), getStringLimit()))
                        .collect(Cc.toL()),
                ctx.getServerRequestId(),
                ctx.getIpInfo().getClientIp(),
                ctx.getUserToken(),
                ms + "." + msRight,
                output.map($ -> $.getRawOrRendered()
                        .collect(x -> x.getHttpCode(), x -> x.getMeta().getHttpCode()))
                        .orElse(0),
                output.map($ -> $.getRawOrRendered().collect(
                        x -> St.raze3dots(json.to(x.getValOrProblem().collectSelf()), getStringLimit()),
                        x -> x.getValue().collect(
                                str -> St.raze3dots(str, getStringLimit()),
                                bytes -> "bytes[" + bytes.length + "]")))
                        .orElse("Output unknown")
        );
    }
}
