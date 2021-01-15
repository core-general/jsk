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
import sk.services.idempotence.IIdempotenceProvider;
import sk.services.idempotence.IdempotenceLockResult;
import sk.services.idempotence.IdempotentValue;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Ti;
import sk.web.annotations.WebIdempotence;
import sk.web.exceptions.IWebExcept;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRenderResult;
import sk.web.renders.WebReplyMeta;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.params.WebIdempotenceParams;

import javax.inject.Inject;
import java.util.Optional;

import static sk.utils.javafixes.TypeWrap.simple;

@Log4j2
public class WebIdempotenceFilter implements WebServerFilter {
    public static final int PRIORITY = WebDdosFilter.PRIORITY + PRIORITY_STEP;

    @Inject IWebExcept except;
    @Inject Optional<WebIdempotenceParams> conf;
    @Inject Optional<IIdempotenceProvider> idempotence;

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        final WebRequestInnerContext ctx = requestContext.getRequestContext();
        final O<WebIdempotence> idemp = ctx.getWebIdempotence();
        boolean lockAcquired = false;
        O<String> oIdempotenceKey = O.empty();
        if (idemp.isPresent()) {
            final WebIdempotence idempotence = idemp.get();
            oIdempotenceKey = idempotence.isParamOrHeader()
                    ? ctx.getParamAsString(idempotence.paramName())
                    : ctx.getRequestHeader(idempotence.paramName());
            if (idempotence.force() && oIdempotenceKey.isEmpty()) {
                return except.returnMissingParameter(idempotence.paramName(), idempotence.isParamOrHeader());
            }
            if (oIdempotenceKey.isPresent()) {
                final IdempotenceLockResult<WebReplyMeta> lock =
                        this.idempotence.orElseThrow(() -> new RuntimeException("No Idempotence Provider set"))
                                .tryLock(oIdempotenceKey.get(), requestContext.getRequestContext().getRequestHash(),
                                        simple(WebReplyMeta.class), conf.get().getLockDuration(),
                                        formRequestInfo(requestContext.getRequestContext()));
                final OneOf<O<IdempotentValue<WebReplyMeta>>, Boolean> cacheStatus = lock.getValueOrLockSuccessStatus();
                if (cacheStatus.isLeft()) {
                    final O<IdempotentValue<WebReplyMeta>> cache = cacheStatus.left();
                    return cache.collect(
                            $ -> WebFilterOutput.rendered(new WebRenderResult($.getMetainfo(), $.getCachedValue())),
                            () -> WebFilterOutput.rawProblem(409, JskProblem.code("idempotence_parameter_mismatch")));
                } else if (cacheStatus.right()) {
                    lockAcquired = true;
                } else {
                    return except.returnMustRetry("Idempotence lock");
                }
            }
        }

        WebFilterOutput webReply = null;
        try {
            webReply = requestContext.getNextInChain().invokeNext();
            if (lockAcquired && oIdempotenceKey.isPresent() && idempotence.isPresent()) {
                final String key = oIdempotenceKey.get();
                final WebRenderResult render = webReply.render(requestContext.getRequestContext().getWebRender(), except);
                if (render.getMeta().isProblem()) {
                    idempotence.get().unlockOrClear(key);
                } else {
                    idempotence.get().cacheValue(key, requestContext.getRequestContext().getRequestHash(),
                            new IdempotentValue<>(render.getMeta(), render.getValue()),
                            conf.get().getCacheDuration());
                }
            }
            return webReply;
        } catch (Exception e) {
            if (lockAcquired && oIdempotenceKey.isPresent()) {
                idempotence.get().unlockOrClear(oIdempotenceKey.get());
            }
            throw e;
        }
    }

    private O<String> formRequestInfo(WebRequestInnerContext requestContext) {
        return O.of(String.format("id:%s; user: %s; ip: %s; url:%s",
                requestContext.getServerRequestId(),
                requestContext.getUserToken().orElse("NONE"),
                requestContext.getIpInfo().getClientIp(),
                requestContext.getUrlPathPart(),
                Ti.yyyyMMddHHmmssSSS.format(requestContext.getStartTime())
        ));
    }

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }

}
