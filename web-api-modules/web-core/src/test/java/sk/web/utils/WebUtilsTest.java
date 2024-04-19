package sk.web.utils;

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

import lombok.Getter;
import org.junit.jupiter.api.Test;
import sk.utils.statics.Cc;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sk.web.utils.WebUtils.dtoTypeProcessorOrFilter;
import static sk.web.utils.WebUtils.simplify;

public class WebUtilsTest {
    @Test
    public void simplifyTest() {
        assertEquals(simplify(int.class), "int");
        assertEquals(simplify(Integer.class), "Integer");
        assertEquals(simplify(WebUtilsTest.class), "WebUtilsTest");
        final Method method = A0.class.getMethods()[0];
        final Type genericParam = method.getGenericParameterTypes()[0];
        final Type genericReturn = method.getGenericReturnType();
        assertEquals(simplify(genericReturn), "Map<Map<Set<Boolean>,Long>,String>");
        assertEquals(simplify(genericParam), "Set<Map<Boolean,String>>");
    }

    @Test
    public void dtoTypeProcessorOrFilterTest() {
        assertArrayEquals(dtoTypeProcessorOrFilter("String").toArray(), Cc.lEmpty().toArray());
        assertArrayEquals(dtoTypeProcessorOrFilter("int").toArray(), Cc.lEmpty().toArray());
        assertArrayEquals(dtoTypeProcessorOrFilter("A").toArray(), Cc.l("A").toArray());
        assertArrayEquals(dtoTypeProcessorOrFilter("A<B>").toArray(), Cc.l("A", "B").toArray());
        assertArrayEquals(dtoTypeProcessorOrFilter("O<String<List,A<B>>,C>").toArray(), Cc.l("A", "B", "C").toArray());
    }

    @Getter
    public static class A0 {
        public Map<Map<Set<Boolean>, Long>, String> a(Set<Map<Boolean, String>> b) {
            return null;
        }
    }
}
