package sk.utils.tuples;

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

import sk.utils.functional.O;
import sk.utils.ifaces.AsList;
import sk.utils.statics.Cc;

import java.util.List;

@SuppressWarnings("unused")
public final class X {
    public static <A> X1<A> x(A a) {
        return new X1<>(a);
    }

    public static <A, B> X2<A, B> x(A a, B b) {
        return new X2<>(a, b);
    }

    public static <A, B, C> X3<A, B, C> x(A a, B b, C c) {
        return new X3<>(a, b, c);
    }

    public static <A, B, C, D> X4<A, B, C, D> x(A a, B b, C c, D d) {
        return new X4<>(a, b, c, d);
    }

    public static <A, B, C, D, E> X5<A, B, C, D, E> x(A a, B b, C c, D d, E e) {
        return new X5<>(a, b, c, d, e);
    }

    public static <A, B, C, D, E, F> X6<A, B, C, D, E, F> x(A a, B b, C c, D d, E e, F f) {
        return new X6<>(a, b, c, d, e, f);
    }

    public static <A, B, C, D, E, F, G> X7<A, B, C, D, E, F, G> x(A a, B b, C c, D d, E e, F f, G g) {
        return new X7<>(a, b, c, d, e, f, g);
    }

    public static List<?> toList(AsList lst) {
        return O.ofNullable(lst).map(AsList::asList).orElseGet(Cc::lEmpty);
    }
}
