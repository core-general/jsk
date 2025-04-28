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
public class OneOrNone<L, R> {
    private static final OneOrNone NONE = new OneOrNone<>(null, null);
    private final L left;
    private final R right;

    public static <L, R> OneOrNone<L, R> left(L value) {return new OneOrNone<>(value, null);}

    public static <L, R> OneOrNone<L, R> right(R value) {return new OneOrNone<>(null, value);}

    public static <L, R> OneOrBoth<L, R> one(OneOf<L, R> val) {
        return new OneOrBoth<>(val.oLeft().orElse(null), val.oRight().orElse(null));
    }

    public static <L, R> OneOrNone<L, R> none() {return NONE;}

    public static <L, R> OneOrNone<L, R> maybeNone(L left, R right) {
        return new OneOrNone<>(left, right);
    }

    public static <L, R> OneOrNone<L, R> mustNone(L left, R right) {
        if (left == null && right == null) {
            return new OneOrNone<>(left, right);
        } else {
            throw new IllegalArgumentException("mustNone is invoked, but at least one argument is not null");
        }
    }

    private OneOrNone(L l, R r) {
        if (l != null && r != null) {
            throw new IllegalArgumentException("Both values are not null");
        }
        left = l;
        right = r;
    }

    public <X, Y> OneOrNone<X, Y> flatMap(F1<OneOf<L, R>, OneOrNone<X, Y>> oneOf, F0<OneOrNone<X, Y>> none) {
        return get().map(oneOf).orElseGet(none::apply);
    }

    public <X, Y> OneOrNone<X, Y> map(F1<? super L, X> lFunc, F1<? super R, Y> rFunc) {
        return new OneOrNone<>(oLeft().map(lFunc).orElse(null), oRight().map(rFunc).orElse(null));
    }

    public <T> OneOrNone<T, R> mapLeft(F1<? super L, T> lFunc) {
        return map(lFunc, a -> a);
    }

    public <T> OneOrNone<L, T> mapRight(F1<? super R, T> rFunc) {
        return map(a -> a, rFunc);
    }

    public <T> T collect(F1<OneOf<L, R>, T> oneOf, F0<T> none) {
        return get().map(oneOf).orElseGet(none::apply);
    }

    @SuppressWarnings("unchecked")
    public O<L> collectSelf() {
        return collect(a -> of(a.collectSelf()), O::empty);
    }

    @SuppressWarnings("unchecked")
    public <T> O<T> collectBoth(F1<? super L, T> bothFunc) {
        return collect(a -> of(a.collectBoth(bothFunc)), O::empty);
    }

    public O<L> collectRight(F1<? super R, L> rFunc) {
        return collect(a -> O.of(a.collectRight(rFunc)), O::empty);
    }

    public O<R> collectLeft(F1<? super L, R> lFunc) {
        return collect(a -> O.of(a.collectLeft(lFunc)), O::empty);
    }

    public void apply(C1<OneOf<L, R>> oneOf, sk.utils.functional.R none) {
        get().ifPresentOrElse(oneOf, none);
    }

    public boolean isNone() {return left == null && right == null;}

    public boolean isLeft() {return left != null;}

    public boolean isRight() {return right != null;}

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

    public O<OneOf<L, R>> get() {
        return left != null || right != null
               ? of(new OneOf<>(left, right))
               : empty();
    }

    @Override
    public String toString() {
        return String.format("{left=%s, right=%s}", left, right);
    }
}
