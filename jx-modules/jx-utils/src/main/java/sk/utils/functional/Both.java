package sk.utils.functional;

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

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Both<L, R> {
    private final L left;
    private final R right;

    public static <L, R> Both<L, R> both(L left, R right) {return new Both<>(left, right);}

    protected Both(L l, R r) {
        if (l == null || r == null) {
            throw new IllegalArgumentException("No nulls allowed");
        }
        left = l;
        right = r;
    }

    public <X, Y> Both<X, Y> flatMap(F2<? super L, ? super R, Both<X, Y>> mapper) {
        return mapper.apply(left, right);
    }

    public <X, Y> Both<X, Y> map(F1<? super L, X> lFunc, F1<? super R, Y> rFunc) {
        return new Both<>(lFunc.apply(left), rFunc.apply(right));
    }

    public <T> Both<T, R> mapLeft(F1<? super L, T> lFunc) {
        return map(lFunc, a -> a);
    }

    public <T> Both<L, T> mapRight(F1<? super R, T> rFunc) {
        return map(a -> a, rFunc);
    }

    public <T> T collect(F2<? super L, ? super R, T> collector) {
        return collector.apply(left, right);
    }

    public L collectSelfLeft() {
        return collect((a, b) -> a);
    }

    public R collectSelfRight() {
        return collect((a, b) -> b);
    }

    public void apply(C2<L, R> consumer) {
        consumer.accept(left, right);
    }

    public L left() {
        return left;
    }

    public R right() {
        return right;
    }
}
