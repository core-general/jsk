package sk.utils.statics;

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

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.Test;
import sk.utils.functional.C2;
import sk.utils.functional.F1;
import sk.utils.functional.O;

import java.lang.reflect.Type;

import static org.junit.Assert.*;

public class ReTest {
    @Test
    @SneakyThrows
    public void getter() {
        {
            F1<Object, Object> getter = Re.getter(A.class.getDeclaredField("myField"));
            C2<Object, Object> setter = Re.setter(A.class.getDeclaredField("myField")).get();

            A a = new A();
            assertNull(getter.apply(a));
            setter.accept(a, "1234");
            assertEquals("1234", getter.apply(a));
        }

        {
            F1<Object, Object> getter = Re.getter(B.class.getDeclaredField("myField"));
            assertEquals(Re.setter(B.class.getDeclaredField("myField")), O.empty());

            B b = new B("123");
            assertEquals("123", getter.apply(b));
        }
    }

    @Data
    public static class ZZZ {
        int a = 5;

        public ZZZ() {
            init();
        }

        private void init() {
            a = 6;
        }
    }

    @Test
    public void getFirstParentParameter() {
        final O<Class<?>> firstParentParameter = Re.getFirstParentParameter(XX.class);
        assertEquals(firstParentParameter.get(), String.class);
    }

    @Test
    public void getParentParameters() {
        Type[] parentParameters = Re.getParentParameters(XX.class).get();
        assertArrayEquals(new Object[]{String.class, Integer.class}, parentParameters);

        parentParameters = Re.getParentParameters(XXX.class).get();
        assertArrayEquals(new Object[]{String.class, Integer.class}, parentParameters);
    }

    public static class X<T1, T2> {
        T1 t1;
        T2 t2;
    }

    public static class XX extends X<String, Integer> {}

    public static class XXX extends XX {}

    @Getter
    @Setter
    public static class A {
        String myField;
    }

    public record B(String myField) {}
}
