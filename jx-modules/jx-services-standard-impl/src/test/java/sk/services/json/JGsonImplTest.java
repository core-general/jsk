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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import sk.services.bean.IServiceLocator;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JGsonImplTest {
    IJson json = new JGsonImpl().init();

    @Test
    public void testNullDeserialization() {
        final InitTester from = json.from("""
                {"a":5,"b":6,"str":null}
                """, InitTester.class);
        assertEquals(from, new InitTester(1, 2, Optional.empty()));
    }

    @Test
    public void testJsonInit() {
        InitTester it = new InitTester();
        final InitTester from = json.from(json.to(it), InitTester.class);
        assertEquals(from.toString(), "JGsonImplTest.InitTester(a=1, b=2, str=Optional.empty)");
    }

    @Test
    public void testJsonSetJson() {
        final String to = json.to(Cc.m("tester", new InitTester()));
        final InitTesterExtended from = json.from(to, InitTesterExtended.class);
        assertEquals("{\"a\":5,\"b\":6}", from.getTester().getRawJson());
        assertEquals(from.toString(), """
                JGsonImplTest.InitTesterExtended(tester=ObjectAndItsJson(object=JGsonImplTest.InitTester(a=1, b=2, str=Optional.empty), rawJson={"a":5,"b":6}))\
                """);
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

    @Test
    public void testBadPolymorphism() {
        var initial = new BadPolymorphismTest.B(1, 2);

        List<String> badPolyMorphs = Cc.l(
                /*Package change*/
                """
                        {"b":2,"a":1,"_jcl_":"sk.services.json.JGsonImplTest$B"}
                        """.trim(),
                """
                        {"b":2,"a":1,"_jcl_":"sk.services.json.B"}
                        """.trim(),
                """
                        {"b":2,"a":1,"_jcl_":"sk.cool.B"}
                        """.trim(),
                /*Name and package change*/
                """
                        {"b":2,"a":1,"_jcl_":"sk.services.json.JGsonImplTest$X"}
                        """.trim(),
                """
                        {"b":2,"a":1,"_jcl_":"sk.services.json.X"}
                        """.trim(),
                """
                        {"b":2,"a":1,"_jcl_":"sk.cool.X"}
                        """.trim()
        );

        for (String badPolyMorph : badPolyMorphs) {
            final BadPolymorphismTest.A from = json.from(badPolyMorph, BadPolymorphismTest.A.class);
            final BadPolymorphismTest.B from2 = json.from(badPolyMorph, BadPolymorphismTest.B.class);
            assertEquals(initial, from);
            assertEquals(initial, from2);
        }
    }


    @NoArgsConstructor
    @AllArgsConstructor
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
    public static class InitTesterExtended {
        ObjectAndItsJson<InitTester> tester;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionalTester {
        private O<String> str;
    }

    public static class BadPolymorphismTest {
        @AllArgsConstructor
        @EqualsAndHashCode(callSuper = false)
        public static class A extends JsonPolymorph {
            private int a;
        }

        @EqualsAndHashCode(callSuper = true)
        public static class B extends A {
            private int b;

            public B(int a, int b) {
                super(a);
                this.b = b;
            }
        }

        @EqualsAndHashCode(callSuper = true)
        public static class C extends A {
            private int c;

            public C(int a, int c) {
                super(a);
                this.c = c;
            }
        }
    }
}
