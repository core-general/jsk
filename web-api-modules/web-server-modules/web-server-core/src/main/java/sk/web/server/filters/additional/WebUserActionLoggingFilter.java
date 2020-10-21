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
import lombok.extern.log4j.Log4j2;
import sk.services.bytes.IBytes;
import sk.services.json.IJson;
import sk.services.kv.IKvUnlimitedStore;
import sk.services.kv.KvAllValues;
import sk.services.kv.KvListItemAll;
import sk.services.kv.keys.KvKey3Categories;
import sk.services.time.ITime;
import sk.utils.functional.Converter;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X3;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestResponseInfo;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.filters.standard.WebRequestLoggingFilter;
import sk.web.server.model.WebProblemWithRequestBodyException;
import sk.web.server.model.WebRequestFinishInfo;
import sk.web.server.model.WebRequestStartInfo;
import sk.web.server.params.WebUserActionLoggerParams;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static sk.utils.functional.O.of;
import static sk.utils.statics.Ti.yyyyMMddHHmmssSSS;

@Log4j2
public abstract class WebUserActionLoggingFilter implements WebServerFilter, WebUserHistoryProvider {
    public static final int PRIORITY = WebRequestLoggingFilter.PRIORITY - PRIORITY_STEP;

    @Inject WebRequestResponseInfo info;
    @Inject WebUserActionLoggerParams conf;
    @Inject IKvUnlimitedStore store;
    @Inject IBytes bytes;
    @Inject ITime times;
    @Inject IJson json;
    @Inject Optional<List<WebUserHistoryAdditionalDataProvider>> additionalProviders = Optional.empty();

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> ctx) {
        WebFilterOutput webFilterOutput = null;
        final O<String> userIdByToken = ctx.getRequestContext().getUserToken().flatMap(this::getUserIdByToken);
        try {
            webFilterOutput = ctx.getNextInChain().invokeNext();
            return webFilterOutput;
        } finally {
            try {
                if (conf.isOn()) {
                    WebFilterOutput fwo = webFilterOutput;
                    userIdByToken.ifPresent(userId -> {
                        final long now = times.now();
                        final LoggingKvMeta loggingKvMeta =
                                new LoggingKvMeta(fwo.getCode(),
                                        ctx.getRequestContext().getRequestType() + ":" + ctx.getRequestContext().getUrlPathPart(),
                                        ctx.getRequestContext().getIpInfo().getClientIp(),
                                        now - times.toMilli(ctx.getRequestContext().getStartTime()));

                        final WebRequestStartInfo requestInfo = info.getRequestRawInfo(ctx);
                        final WebRequestFinishInfo responseInfo = info.getResponseRawInfo(ctx, O.of(fwo));
                        WebRequestFullInfo full = new WebRequestFullInfo(requestInfo, responseInfo,
                                O.of(additionalProviders).stream().flatMap($ -> $.stream())
                                        .map($ -> X.x($.getName(), $.provideAdditionalData(ctx)))
                                        .collect(Cc.toMX2()));

                        final O<byte[]> zipped = getRawValueConverter().convertThere(of(OneOf.left(full)));

                        store.trySaveNewObjectAndRaw(new LoggingKvKey(userId, times.nowZ()),
                                new KvAllValues<>(loggingKvMeta, zipped,
                                        O.of(ctx.getRequestContext().getStartTime().plus(conf.getTtl()))));
                    });
                }
            } catch (WebProblemWithRequestBodyException e) {
                //ignoring, since we already have it in WebServerCore
            } catch (Exception e) {
                //we are just catching exception without rethrow since logging is optional and we do not need to
                log.warn("BAD SAVE REQUEST", e);
            }
        }
    }

    public List<X3<WebUserActionLoggingFilter.LoggingKvMeta, String, ZonedDateTime>> getRenderedUserHistory(String userId,
            O<ZonedDateTime> from, O<ZonedDateTime> to,
            int maxCount, boolean descending) {
        final List<KvListItemAll<String>> items = store.getRawVersionedListBetweenCategories(new LoggingKvKey(userId, null),
                from.map(yyyyMMddHHmmssSSS::format),
                to.map(yyyyMMddHHmmssSSS::format),
                maxCount,
                descending
        );
        return items.stream()
                .filter($ -> $.getRawValue().isPresent())
                .map($ -> {
                    final String output = getRawValueConverter().convertBack($.getRawValue()).get()
                            .mapLeft(wri -> info.generateRequestString(wri.request) + info.generateResponseString(wri.response))
                            .collectBoth(str -> str.replace("\n\n\n\n", "\n")
                                    .replace("\n\n\n", "\n")
                                    .replace("\n\n", "\n"));
                    return X.x(json.from($.getValue(), LoggingKvMeta.class), output, $.getCreated());
                })
                .collect(Cc.toL());
    }

    public List<WebRequestFullInfo> getFullUserHistory(String userId,
            O<ZonedDateTime> from, O<ZonedDateTime> to,
            int maxCount, boolean descending) {
        final List<KvListItemAll<String>> items = store.getRawVersionedListBetweenCategories(new LoggingKvKey(userId, null),
                from.map(yyyyMMddHHmmssSSS::format),
                to.map(yyyyMMddHHmmssSSS::format),
                maxCount,
                descending
        );
        return items.stream()
                .filter($ -> $.getRawValue().isPresent())
                .map($ -> getRawValueConverter().convertBack($.getRawValue()).get())
                .filter($ -> $.isLeft())
                .map($ -> $.left())
                .collect(Cc.toL());
    }

    public abstract O<String> getUserIdByToken(String userToken);

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }

    private Converter<O<OneOf<WebRequestFullInfo, String>>, O<byte[]>> getRawValueConverter() {
        return new Converter<O<OneOf<WebRequestFullInfo, String>>, O<byte[]>>() {
            @Override
            public O<byte[]> convertThere(O<OneOf<WebRequestFullInfo, String>> in) {
                return in.flatMap($ -> $.mapLeft(wr -> json.to(wr)).collectBoth(str -> bytes.zipString(str)));
            }

            @Override
            public O<OneOf<WebRequestFullInfo, String>> convertBack(O<byte[]> in) {
                return in.flatMap($ -> bytes.unZipString($))
                        .map($ -> json.validate($)
                                ? OneOf.left(json.from($, WebRequestFullInfo.class))
                                : OneOf.right($));
            }
        };
    }

    @Data
    private static class LoggingKvKey implements KvKey3Categories {
        final String key1 = "REQUEST_LOG";
        final O<String> key2;//userId
        final O<String> key3;//datetime
        final String defaultValue = "";

        public LoggingKvKey(String userId, ZonedDateTime created) {
            this.key2 = O.of(userId);
            key3 = O.ofNull(created).map($ -> yyyyMMddHHmmssSSS.format($));
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
