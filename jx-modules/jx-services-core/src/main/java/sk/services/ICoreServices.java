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

public interface ICoreServices {
    sk.services.async.IAsync async();

    sk.services.bytes.IBytes bytes();

    sk.services.http.IHttp http();

    sk.services.ids.IIds ids();

    sk.services.json.IJson json();

    sk.services.log.ILog iLog();

    sk.services.rand.IRand rand();

    sk.services.rescache.IResCache resCache();

    sk.services.retry.IRepeat repeat();

    sk.services.time.ITime times();

    sk.services.free.IFree free();

    sk.services.except.IExcept except();

    sk.services.async.ISizedSemaphore sizedSemaphore();
}
