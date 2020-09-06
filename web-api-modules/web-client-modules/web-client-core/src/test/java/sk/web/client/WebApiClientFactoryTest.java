package sk.web.client;

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

import org.junit.Test;
import sk.utils.statics.Cc;

import static org.junit.Assert.assertEquals;

public class WebApiClientFactoryTest {
    @Test
    public void tunePathParams() {
        final WebClientFactory toTest = new WebClientFactory();

        assertEquals("12345", toTest.tunePathParams(":abc", Cc.m("abc", "12345", "bcd", "67890")));
        assertEquals("/12345", toTest.tunePathParams("/:abc", Cc.m("abc", "12345", "bcd", "67890")));
        assertEquals("12345/abc", toTest.tunePathParams(":abc/abc", Cc.m("abc", "12345", "bcd", "67890")));
        assertEquals("12345/abc/12345", toTest.tunePathParams(":abc/abc/:abc", Cc.m("abc", "12345", "bcd", "67890")));
        assertEquals("12345/abc/67890/a", toTest.tunePathParams(":abc/abc/:bcd/a", Cc.m("abc", "12345", "bcd", "67890")));


        assertEquals("http://localhost:8080",
                toTest.tunePathParams("http://localhost:8080", Cc.m("abc", "12345", "bcd", "67890")));
        assertEquals("https://localhost:8080",
                toTest.tunePathParams("https://localhost:8080", Cc.m("abc", "12345", "bcd", "67890")));
        assertEquals("https://localhost:8080/12345",
                toTest.tunePathParams("https://localhost:8080/:abc", Cc.m("abc", "12345", "bcd", "67890")));
        assertEquals("https://localhost:8080/12345/abc/67890/a",
                toTest.tunePathParams("https://localhost:8080/:abc/abc/:bcd/a", Cc.m("abc", "12345", "bcd", "67890")));
    }
}
