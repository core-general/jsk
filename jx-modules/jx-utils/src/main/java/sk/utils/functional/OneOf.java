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

import java.util.function.Consumer;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class OneOf<L, R> {
    private final L left;
    private final R right;

    public static <L, R> OneOf<L, R> left(L value) {return new OneOf<>(value, null);}

    public static <L, R> OneOf<L, R> right(R value) {return new OneOf<>(null, value);}

    private OneOf(L l, R r) {
        if (l == null && r == null) {
            throw new IllegalArgumentException("Both values are null");
        }
        left = l;
        right = r;
    }

    public <X, Y> OneOf<X, Y> flatMap(F1<? super L, OneOf<X, Y>> lFunc, F1<? super R, OneOf<X, Y>> rFunc) {
        return O.ofNull(left).map(lFunc).or(() -> O.ofNull(right).map(rFunc)).get();
    }

    public <X, Y> OneOf<X, Y> map(F1<? super L, ? extends X> lFunc, F1<? super R, ? extends Y> rFunc) {
        return new OneOf<>(O.ofNull(left).map(lFunc).orElse(null), O.ofNull(right).map(rFunc).orElse(null));
    }

    public <T> OneOf<T, R> mapLeft(F1<? super L, ? extends T> lFunc) {
        return new OneOf<>(O.ofNull(left).map(lFunc).orElse(null), right);
    }

    public <T> OneOf<L, T> mapRight(F1<? super R, ? extends T> rFunc) {
        return new OneOf<>(left, O.ofNull(right).map(rFunc).orElse(null));
    }

    public void applyLeft(C1<? super L> lFunc) {
        O.ofNull(left).ifPresent(lFunc);
    }

    public void applyRight(C1<? super R> rFunc) {
        O.ofNull(right).ifPresent(rFunc);
    }

    @SuppressWarnings("unchecked")
    public L collectSelf() {
        return collect(a -> a, a -> (L) a);
    }

    public L collectRight(F1<? super R, L> rFunc) {
        return collect(a -> a, rFunc);
    }

    public <T> T collect(F1<? super L, T> lFunc, F1<? super R, T> rFunc) {
        O<? extends T> t = O.ofNull(left).map(lFunc);
        if (t.isPresent()) {
            return t.get();
        } else {
            return O.ofNull(right).map(rFunc).get();
        }
    }

    public void apply(Consumer<? super L> lFunc, Consumer<? super R> rFunc) {
        O.ofNull(left).ifPresent(lFunc);
        O.ofNull(right).ifPresent(rFunc);
    }

    public boolean isLeft() {return left != null;}

    public boolean isRight() {return right != null;}

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
