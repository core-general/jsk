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

import lombok.EqualsAndHashCode;

@SuppressWarnings({"unused", "WeakerAccess"})
@EqualsAndHashCode
public class OneOf<L, R> {
    private final L left;
    private final R right;

    public static <L, R> OneOf<L, R> left(L value) {return new OneOf<>(value, null);}

    public static <L, R> OneOf<L, R> right(R value) {return new OneOf<>(null, value);}

    OneOf(L l, R r) {
        if (l == null && r == null) {
            throw new IllegalArgumentException("Both values are null");
        }
        left = l;
        right = r;
    }

    public <X, Y> OneOf<X, Y> flatMap(F1<? super L, OneOf<X, Y>> lFunc, F1<? super R, OneOf<X, Y>> rFunc) {
        return oLeft().map(lFunc).or(() -> oRight().map(rFunc)).get();
    }

    public <X, Y> OneOf<X, Y> map(F1<? super L, X> lFunc, F1<? super R, Y> rFunc) {
        return new OneOf<>(oLeft().map(lFunc).orElse(null), oRight().map(rFunc).orElse(null));
    }

    public <T> OneOf<T, R> mapLeft(F1<? super L, T> lFunc) {
        return map(lFunc, a -> a);
    }

    public <T> OneOf<L, T> mapRight(F1<? super R, T> rFunc) {
        return map(a -> a, rFunc::apply);
    }

    public <T> T collect(F1<? super L, T> lFunc, F1<? super R, T> rFunc) {
        return oLeft().map(lFunc).or(() -> oRight().map(rFunc)).get();
    }

    @SuppressWarnings("unchecked")
    public L collectSelf() {
        return collect(a -> a, a -> (L) a);
    }

    @SuppressWarnings("unchecked")
    public <T> T collectBoth(F1<? super L, T> bothFunc) {
        return collect(bothFunc::apply, r -> bothFunc.apply((L) r));
    }

    public L collectRight(F1<? super R, L> rFunc) {
        return collect(a -> a, rFunc);
    }

    public R collectLeft(F1<? super L, R> lFunc) {
        return collect(lFunc, a -> a);
    }

    public void apply(C1<? super L> lFunc, C1<? super R> rFunc) {
        O.ofNull(left).ifPresent(lFunc);
        O.ofNull(right).ifPresent(rFunc);
    }

    public boolean isLeft() {return left != null;}

    public void ifLeft(C1<L> lFunc) {
        if (isLeft()) {
            lFunc.accept(left);
        }
    }

    public boolean isRight() {return right != null;}

    public void ifRight(C1<R> rFunc) {
        if (isRight()) {
            rFunc.accept(right);
        }
    }

    public L left() {
        return O.ofNull(left).get();
    }

    public R right() {
        return O.ofNull(right).get();
    }

    public O<L> oLeft() {
        return O.ofNull(left);
    }

    public O<R> oRight() {
        return O.ofNull(right);
    }

    @Override
    public String toString() {
        return String.format("{left=%s, right=%s}", left, right);
    }
}
