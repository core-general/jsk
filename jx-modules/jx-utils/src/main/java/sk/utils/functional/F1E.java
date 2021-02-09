package sk.utils.functional;

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

import java.util.Objects;

@SuppressWarnings("unused")
@FunctionalInterface
public interface F1E<A, B> {
    B apply(A t) throws Exception;

    default <V> F1E<V, B> compose(F1E<? super V, ? extends A> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    default <V> F1E<A, V> andThen(F1E<? super B, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A t) -> after.apply(apply(t));
    }
}
