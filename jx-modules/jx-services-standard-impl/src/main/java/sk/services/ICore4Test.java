package sk.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import lombok.Getter;
import lombok.experimental.Accessors;
import sk.services.async.AsyncImpl;
import sk.services.async.IAsync;
import sk.services.async.ISizedSemaphore;
import sk.services.async.ISizedSemaphoreImpl;
import sk.services.bytes.BytesImpl;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.free.Freemarker;
import sk.services.free.IFree;
import sk.services.http.HttpImpl;
import sk.services.http.IHttp;
import sk.services.ids.IIds;
import sk.services.ids.IdsImpl;
import sk.services.json.IJson;
import sk.services.json.JGsonImpl;
import sk.services.log.ILog;
import sk.services.log.ILogConsoleImpl;
import sk.services.rand.IRand;
import sk.services.rand.RandTestImpl;
import sk.services.rescache.IResCache;
import sk.services.rescache.ResCacheImpl;
import sk.services.retry.IRepeat;
import sk.services.retry.RepeatImpl;
import sk.services.time.ITimeSetter;
import sk.services.time.UtcSettableTimeUtilImpl;
import sk.utils.functional.O;

@Getter
@Accessors(chain = true, fluent = true)
public class ICore4Test implements ICoreServices {
    private static ICore4Test cur;

    public static ICoreServices services() {
        return cur == null ? (cur = new ICore4Test()) : cur;
    }

    private IAsync async = new AsyncImpl();
    private IBytes bytes = new BytesImpl();
    private IHttp http = new HttpImpl();
    private IRand rand = new RandTestImpl();
    private IIds ids = new IdsImpl(rand, bytes);
    private ITimeSetter times = new UtcSettableTimeUtilImpl();
    private IJson json = new JGsonImpl(O.empty(), times).init();
    private ILog iLog = new ILogConsoleImpl(json);

    private IResCache resCache = new ResCacheImpl();
    private IRepeat repeat = new RepeatImpl(async);
    private IFree free = new Freemarker();
    private ISizedSemaphore sizedSemaphore = new ISizedSemaphoreImpl(Runtime.getRuntime().maxMemory() / 5, 50L, async);
    private IExcept except = new IExcept() {};
}
