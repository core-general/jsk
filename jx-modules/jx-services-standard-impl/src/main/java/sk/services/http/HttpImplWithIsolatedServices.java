package sk.services.http;

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

import sk.services.async.AsyncImpl;
import sk.services.bytes.BytesImpl;
import sk.services.ids.IdsImpl;
import sk.services.retry.RepeatImpl;
import sk.services.time.TimeUtcImpl;

public class HttpImplWithIsolatedServices extends HttpImpl {
    public HttpImplWithIsolatedServices() {
        super(new RepeatImpl(new AsyncImpl()), new TimeUtcImpl(), new AsyncImpl(), new IdsImpl(), new BytesImpl());
    }
}
