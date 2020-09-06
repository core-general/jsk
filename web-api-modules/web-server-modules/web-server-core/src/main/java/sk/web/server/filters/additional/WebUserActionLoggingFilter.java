package sk.web.server.filters.additional;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sk.services.bytes.IBytes;
import sk.services.json.IJson;
import sk.services.kv.IKvUnlimitedStore;
import sk.services.kv.KvAllValues;
import sk.services.kv.keys.KvKey3Categories;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.statics.Ti;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestResponseInfo;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.filters.standard.WebRequestLoggingFilter;
import sk.web.server.model.WebProblemWithRequestBodyException;
import sk.web.server.params.WebUserActionLoggerParams;

import javax.inject.Inject;

@Log4j2
public abstract class WebUserActionLoggingFilter implements WebServerFilter {
    public static final int PRIORITY = WebRequestLoggingFilter.PRIORITY - PRIORITY_STEP;

    @Inject WebRequestResponseInfo info;
    @Inject WebUserActionLoggerParams conf;
    @Inject IKvUnlimitedStore store;
    @Inject IBytes bytes;
    @Inject ITime times;
    @Inject IJson json;

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> ctx) {
        WebFilterOutput webFilterOutput = null;
        try {
            webFilterOutput = ctx.getNextInChain().invokeNext();
            return webFilterOutput;
        } finally {
            try {
                if (conf.isOn()) {
                    WebFilterOutput fwo = webFilterOutput;
                    ctx.getRequestContext().getUserToken().flatMap(this::getUserIdByToken).ifPresent(userId -> {
                        final long now = times.now();
                        final LoggingKvMeta loggingKvMeta =
                                new LoggingKvMeta(fwo.getCode(),
                                        ctx.getRequestContext().getRequestType() + ":" + ctx.getRequestContext().getUrlPathPart(),
                                        ctx.getRequestContext().getIpInfo().getClientIp(),
                                        now - times.toMilli(ctx.getRequestContext().getStartTime()));

                        final String requestInfo = info.getRequestInfo(ctx);
                        final String responseInfo = info.getResponseInfo(ctx, O.of(fwo));
                        final O<byte[]> zipped = bytes.zipString(requestInfo + "\n" + responseInfo);

                        store.trySaveNewObjectAndRaw(new LoggingKvKey(userId),
                                new KvAllValues<>(loggingKvMeta, zipped,
                                        O.of(ctx.getRequestContext().getStartTime().plus(conf.getTtl()))));
                    });
                }
            } catch (WebProblemWithRequestBodyException e) {
                //ignoring, since we already have it in WebServerCore
            } catch (Exception e) {
                //we are just catching exception without rethrow since logging is optional and we do not need to
                log.warn("", e);
            }
        }
    }

    public abstract O<String> getUserIdByToken(String userToken);

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }

    @Data
    @RequiredArgsConstructor
    private class LoggingKvKey implements KvKey3Categories {
        final String key1 = "REQUEST_LOG";
        final O<String> key2;//userId
        final O<String> key3 = O.of(Ti.yyyyMMddHHmmssSSS.format(times.nowZ()));
        final String defaultValue = "";

        public LoggingKvKey(String userId) {
            this.key2 = O.of(userId);
        }
    }

    @Data
    @AllArgsConstructor
    public static class LoggingKvMeta {
        int httpCode;
        String address;
        String ip;
        long ms;
    }
}
