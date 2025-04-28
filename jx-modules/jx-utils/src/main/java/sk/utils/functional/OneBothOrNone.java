package sk.utils.functional;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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

import java.util.Optional;

public class OneBothOrNone<L, R> {
    public static final OneBothOrNone NONE = new OneBothOrNone<>(null, null);
    private final L left;
    private final R right;

    public static <L, R> OneBothOrNone<L, R> left(L value) {return new OneBothOrNone<>(value, null);}

    public static <L, R> OneBothOrNone<L, R> right(R value) {return new OneBothOrNone<>(null, value);}

    public static <L, R> OneBothOrNone<L, R> any(L left, R right) {return new OneBothOrNone<>(left, right);}

    public static <L, R> OneBothOrNone<L, R> any(O<L> left, O<R> right) {
        return new OneBothOrNone<>(O.of(left).orElse(null), O.of(right).orElse(null));
    }

    public static <L, R> OneBothOrNone<L, R> any(Optional<L> left, Optional<R> right) {
        return new OneBothOrNone<>(O.of(left).orElse(null), O.of(right).orElse(null));
    }

    public static <L, R> OneBothOrNone<L, R> both(Both<L, R> val) {
        return new OneBothOrNone<>(val.left(), val.right());
    }

    public static <L, R> OneBothOrNone<L, R> bothOrNone(BothOrNone<L, R> val) {
        return new OneBothOrNone<>(val.left(), val.right());
    }

    public static <L, R> OneBothOrNone<L, R> one(OneOf<L, R> val) {
        return new OneBothOrNone<>(val.oLeft().orElse(null), val.oRight().orElse(null));
    }

    public static <L, R> OneBothOrNone<L, R> none() {return NONE;}

    protected OneBothOrNone(L l, R r) {
        left = l;
        right = r;
    }

    public <X, Y> OneBothOrNone<X, Y> flatMap(
            F1<OneOf<L, R>, OneBothOrNone<X, Y>> oneOf,
            F1<Both<L, R>, OneBothOrNone<X, Y>> both,
            F0<OneBothOrNone<X, Y>> none
    ) {
        return get().collect(oneOf,
                lrBothOrNone -> lrBothOrNone.isBoth() ? both.apply(lrBothOrNone.getBoth()) : none.apply());
    }

    public OneBothOrNone<L, R> combine(OneBothOrNone<L, R> other, F2<O<L>, O<L>, O<L>> lMapping, F2<O<R>, O<R>, O<R>> rMapping) {
        O<L> oLeft = lMapping.apply(oLeft(), other.oLeft());
        O<R> oRight = rMapping.apply(oRight(), other.oRight());
        return any(oLeft, oRight);
    }

    public <X, Y> OneBothOrNone<X, Y> map(F1<? super L, X> lFunc, F1<? super R, Y> rFunc) {
        return any(lFunc.apply(left), rFunc.apply(right));
    }

    public <T> OneBothOrNone<T, R> mapLeft(F1<? super L, T> lFunc) {
        return map(lFunc, a -> a);
    }

    public <T> OneBothOrNone<L, T> mapRight(F1<? super R, T> rFunc) {
        return map(a -> a, rFunc);
    }

    public <T> T collect(
            F1<OneOf<L, R>, T> oneOf,
            F1<Both<L, R>, T> both,
            F0<T> none
    ) {
        return get().collect(oneOf,
                bothOrNone -> bothOrNone.isBoth() ? both.apply(bothOrNone.getBoth()) : none.apply());
    }

    public void apply(C1<OneOf<L, R>> lFunc, C1<BothOrNone<L, R>> rFunc) {
        get().apply(lFunc, rFunc);
    }

    public OneOf<OneOf<L, R>, BothOrNone<L, R>> get() {
        return left != null && right != null
               ? OneOf.right(BothOrNone.both(left, right))
               : left == null && right == null
                 ? OneOf.right(BothOrNone.none())
                 : left != null
                   ? OneOf.left(OneOf.left(left))
                   : OneOf.left(OneOf.right(right));
    }

    public boolean isNone() {return left == null && right == null;}

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
