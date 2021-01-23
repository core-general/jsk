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

import static sk.utils.functional.O.*;

@SuppressWarnings({"unused", "WeakerAccess"})
@EqualsAndHashCode
public class BothOrNone<L, R> {
    public static final BothOrNone NONE = new BothOrNone<>(null, null);
    private final L left;
    private final R right;

    public static <L, R> BothOrNone<L, R> both(L left, R right) {return new BothOrNone<>(left, right);}

    public static <L, R> BothOrNone<L, R> both(Both<L, R> both) {return new BothOrNone<>(both.left(), both.right());}

    public static <L, R> BothOrNone<L, R> none() {return NONE;}

    private BothOrNone(L l, R r) {
        if (l != null && r == null || l == null && r != null) {
            throw new IllegalArgumentException("Either both null either all non null");
        }
        left = l;
        right = r;
    }

    public <X, Y> BothOrNone<X, Y> flatMap(F1<Both<L, R>, BothOrNone<X, Y>> oneOf, F0<BothOrNone<X, Y>> none) {
        return get().map(oneOf).orElseGet(none::apply);
    }

    public <X, Y> BothOrNone<X, Y> map(F1<? super L, X> lFunc, F1<? super R, Y> rFunc) {
        return new BothOrNone<>(oLeft().map(lFunc).orElse(null), oRight().map(rFunc).orElse(null));
    }

    public <T> BothOrNone<T, R> mapLeft(F1<? super L, T> lFunc) {
        return map(lFunc, a -> a);
    }

    public <T> BothOrNone<L, T> mapRight(F1<? super R, T> rFunc) {
        return map(a -> a, rFunc);
    }

    public <T> T collect(F1<Both<L, R>, T> both, F0<T> none) {
        return get().map(both).orElseGet(none::apply);
    }

    @SuppressWarnings("unchecked")
    public O<L> collectSelfLeft() {
        return collect(a -> of(a.collectSelfLeft()), O::empty);
    }

    public O<R> collectSelfRight() {
        return collect(a -> of(a.collectSelfRight()), O::empty);
    }

    public void apply(C1<Both<L, R>> oneOf, sk.utils.functional.R none) {
        get().ifPresentOrElse(oneOf, none);
    }

    public boolean isNone() {return left == null && right == null;}

    public boolean isBoth() {return left != null && right != null;}

    public L left() {
        return ofNull(left).get();
    }

    public R right() {
        return ofNull(right).get();
    }

    public O<L> oLeft() {
        return ofNull(left);
    }

    public O<R> oRight() {
        return ofNull(right);
    }

    public O<Both<L, R>> get() {
        return left != null && right != null
                ? of(Both.both(left, right))
                : empty();
    }

    @Override
    public String toString() {
        return String.format("{left=%s, right=%s}", left, right);
    }
}
