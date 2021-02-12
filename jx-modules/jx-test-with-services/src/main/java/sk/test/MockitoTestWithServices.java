package sk.test;

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

import org.mockito.Spy;
import sk.services.ICore4Test;
import sk.services.ICoreServices;
import sk.services.async.IAsync;
import sk.services.async.ISizedSemaphore;
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
import sk.utils.statics.fortest.Profiler;

public class MockitoTestWithServices extends MockitoTest {
    {
        Profiler.mark("FULL", "pre_start");
    }

    private final ICoreServices coreServices = createCoreServices();

    @Spy protected IAsync async = coreServices.async();
    @Spy protected IBytes bytes = coreServices.bytes();
    @Spy protected IHttp http = coreServices.http();
    @Spy protected IRand rand = coreServices.rand();
    @Spy protected IIds ids = coreServices.ids();
    @Spy protected ITime times = coreServices.times();
    @Spy protected IJson json = coreServices.json();
    @Spy protected ILog iLog = coreServices.iLog();

    @Spy protected IResCache resCache = coreServices.resCache();
    @Spy protected IRepeat repeat = coreServices.repeat();
    @Spy protected IFree free = coreServices.free();
    @Spy protected ISizedSemaphore sizedSemaphore = coreServices.sizedSemaphore();
    @Spy protected IExcept except = coreServices.except();

    protected ICoreServices createCoreServices() {
        return coreServices == null ? ICore4Test.services() : coreServices;
    }
}
