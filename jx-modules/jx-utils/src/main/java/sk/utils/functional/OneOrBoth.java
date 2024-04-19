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
public class OneOrBoth<L, R> {
    private final L left;
    private final R right;

    public static <L, R> OneOrBoth<L, R> left(L value) {return new OneOrBoth<>(value, null);}

    public static <L, R> OneOrBoth<L, R> right(R value) {return new OneOrBoth<>(null, value);}

    public static <L, R> OneOrBoth<L, R> mustBoth(L left, R right) {
        if (left == null) {
            throw new IllegalArgumentException("mustBoth is invoked, but left is null");
        }
        if (right == null) {
            throw new IllegalArgumentException("mustBoth is invoked, but right is null");
        }

        return new OneOrBoth<>(left, right);
    }

    public static <L, R> OneOrBoth<L, R> maybeBoth(L left, R right) {return new OneOrBoth<>(left, right);}

    public static <L, R> OneOrBoth<L, R> one(OneOf<L, R> val) {
        return new OneOrBoth<>(val.oLeft().orElse(null), val.oRight().orElse(null));
    }

    public static <L, R> OneOrBoth<L, R> both(Both<L, R> val) {
        return new OneOrBoth<>(val.left(), val.right());
    }

    protected OneOrBoth(L l, R r) {
        if (l == null && r == null) {
            throw new IllegalArgumentException("Both values are null");
        }
        left = l;
        right = r;
    }

    public <X, Y> OneOrBoth<X, Y> flatMap(
            F1<OneOf<L, R>, OneOrBoth<X, Y>> oneOf,
            F1<Both<L, R>, OneOrBoth<X, Y>> both
    ) {
        return get().collect(oneOf, both);
    }

    public <X, Y> OneOrBoth<X, Y> map(F1<? super L, X> lFunc, F1<? super R, Y> rFunc) {
        return get()
                .map($ -> $.map(lFunc, rFunc), $ -> $.map(lFunc, rFunc))
                .collect(OneOrBoth::one, OneOrBoth::both);
    }

    public <T> OneOrBoth<T, R> mapLeft(F1<? super L, T> lFunc) {
        return map(lFunc, a -> a);
    }

    public <T> OneOrBoth<L, T> mapRight(F1<? super R, T> rFunc) {
        return map(a -> a, rFunc);
    }

    public <T> T collect(
            F1<OneOf<L, R>, T> oneOf,
            F1<Both<L, R>, T> both
    ) {
        return get().collect(oneOf, both);
    }

    @SuppressWarnings("unchecked")
    public L collectSelfLeft() {
        return this.collect(OneOf::collectSelf, Both::left);
    }

    @SuppressWarnings("unchecked")
    public R collectSelfRight() {
        return this.collect(lrOneOf -> (R) lrOneOf.collectSelf(), Both::right);
    }

    @SuppressWarnings("unchecked")
    public <T> T collectBoth(F1<? super L, T> bothFunc) {
        return this.collect(oneOf -> oneOf.collectBoth(bothFunc), (a) -> bothFunc.apply(a.left()));
    }

    public void apply(C1<OneOf<L, R>> lFunc, C1<Both<L, R>> rFunc) {
        get().apply(lFunc, rFunc);
    }

    public OneOf<OneOf<L, R>, Both<L, R>> get() {
        return left != null && right != null
                ? OneOf.right(Both.both(left, right))
                : left != null
                        ? OneOf.left(OneOf.left(left))
                        : OneOf.left(OneOf.right(right));
    }

    public boolean isBoth() {return left != null && right != null;}

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
