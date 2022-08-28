package sk.services.json;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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
import lombok.NoArgsConstructor;
import org.junit.Test;
import sk.services.bean.IServiceLocator;
import sk.utils.functional.O;
import sk.utils.statics.Ex;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class JGsonImplTest {
    IJson json = new JGsonImpl().init();

    @Test
    public void testJsonInit() {
        InitTester it = new InitTester();
        final InitTester from = json.from(json.to(it), InitTester.class);
        assertEquals(from.toString(), "JGsonImplTest.InitTester(a=1, b=2, str=Optional.empty)");
    }

    @Test
    public void testEmptyFields() {
        OptionalTester tester = json.from(json.to(new OptionalTester()), OptionalTester.class);
        assertEquals(tester.str, O.empty());
        tester = json.from(json.to(new OptionalTester(O.of("123"))), OptionalTester.class);
        assertEquals(tester.str, O.of("123"));
    }


    @Test
    public void testJsonPath() {
        record X(int a, String b) {}

        var xExceptionOneOf = json.jsonPath("""
                {
                "e": {
                        "a":5,
                        "b":"b",
                        "c":true
                    }
                }
                """, ctx -> {
            final X read = ctx.read("$.e", X.class);
            return read;
        });
        assertEquals(xExceptionOneOf.collect($ -> $, $ -> Ex.thRow($)), new X(5, "b"));
    }


    @NoArgsConstructor
    @Data
    public static class InitTester implements IJsonInitialized {
        private int a = 5;
        private int b = 6;
        Optional<String> str;

        @Override
        public void initAfterJsonDeserialize(O<IServiceLocator> serviceProvider) {
            assertEquals(serviceProvider, O.empty());
            a = 1;
            b = 2;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionalTester {
        private O<String> str;
    }
}
