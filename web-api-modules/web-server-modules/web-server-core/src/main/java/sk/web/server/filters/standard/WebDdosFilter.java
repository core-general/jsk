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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.log4j.Log4j2;
import sk.exceptions.JskProblem;
import sk.services.nodeinfo.IBeanInfoSubscriber;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;
import sk.web.renders.WebFilterOutput;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.params.WebDdosParams;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class WebDdosFilter implements WebServerFilter, IBeanInfoSubscriber<Map<String, String>> {
    public static final int PRIORITY = WebDefaultHeadersFilter.PRIORITY + PRIORITY_STEP;

    @Inject WebDdosParams conf;

    Cache<String, Class<Void>> jail;
    LoadingCache<String, AtomicInteger> court;

    @PostConstruct
    public void init() {
        court = Caffeine.newBuilder()
                .expireAfterWrite(conf.getUserInCourtPeriod())
                .build(key -> new AtomicInteger(conf.getUserRequestsAllowedInCourt()));
        jail = Caffeine.newBuilder()
                .expireAfterWrite(conf.getUserInJailTime())
                .build();
    }

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        if (conf.isDdosCourtEnabled()) {
            final String clientIp = requestContext.getRequestContext().getIpInfo().getClientIp();
            if (jail.getIfPresent(clientIp) != null) {
                log.error("ddoser:" + clientIp + "");
                return WebFilterOutput.rawProblem(403, JskProblem.code("forbidden"));
            } else {
                @SuppressWarnings("ConstantConditions")
                long currentRequestCount = court.get(clientIp).decrementAndGet();
                if (currentRequestCount <= 0) {
                    jail.put(clientIp, Void.class);
                }
            }
        }

        return requestContext.getNextInChain().invokeNext();
    }

    @Override
    public X2<String, Map<String, String>> gatherDiagnosticInfo() {
        return X.x("DDOS_JAIL", Cc.m("jail_size", jail.estimatedSize() + "", "court_size", court.estimatedSize() + ""));
    }

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }
}
