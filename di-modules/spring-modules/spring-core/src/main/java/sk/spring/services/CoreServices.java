package sk.spring.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import sk.services.ICoreServices;
import sk.services.async.IAsync;
import sk.services.async.ISizedSemaphore;
import sk.services.bean.IServiceLocator;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.free.IFree;
import sk.services.http.IHttp;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.log.ILog;
import sk.services.rand.IRand;
import sk.services.rescache.IResCache;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;

@SuppressWarnings({"unused"})
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true, fluent = true)
@Order(5)
public final class CoreServices implements ApplicationListener<ContextRefreshedEvent>, ICoreServices {
    private @Inject IServiceLocator serviceProvider;

    private @Inject IAsync async;
    private @Inject IBytes bytes;
    private @Inject IHttp http;
    private @Inject IIds ids;
    private @Inject IJson json;
    private ILog iLog;
    private @Inject IRand rand;
    private @Inject IResCache resCache;
    private @Inject IRepeat repeat;
    private @Inject ITime times;
    private @Inject IFree free;
    private @Inject ISizedSemaphore sizedSemaphore;
    private @Inject IExcept except;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent applicationEvent) {
        iLog = serviceProvider.getService(ILog.class).get();
    }
}
