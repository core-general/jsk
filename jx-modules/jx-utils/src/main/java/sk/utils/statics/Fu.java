package sk.utils.statics;

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

import lombok.extern.log4j.Log4j2;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.functional.R;
import sk.utils.functional.RE;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Comparator.*;

@Log4j2
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Fu {

    public static R emptyR() {
        return () -> {
        };
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> C1<T> emptyC() {
        return t -> {
        };
    }

    public static <T> O<T> coalesce(T a, T b) {
        return O.ofNull(a == null ? b : a);
    }

    public static <T> O<T> coalesce(T a, T b, T c) {
        return O.ofNull(a != null ? a : (b != null ? b : c));
    }

    public static <A, B> O<X2<A, B>> bothPresent(O<A> a, O<B> b) {
        return a.isPresent() && b.isPresent() ? O.of(X.x(a.get(), b.get())) : O.empty();
    }

    public static boolean isTrue(Boolean someBoolean) {
        return someBoolean != null && someBoolean;
    }

    public static <T> boolean falseOnException(BooleanSupplier predicate) {
        try {
            return predicate.getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    public static R run4ever(RE toRun) {
        return run4ever(toRun, emptyC(), null);
    }

    public static R run4ever(RE toRun, C1<Throwable> onThrowable, Supplier<Boolean> shouldFinish) {
        return () -> {
            while (shouldFinish == null || !shouldFinish.get()) {
                try {
                    toRun.run();
                } catch (Throwable e) {
                    onThrowable.accept(e);
                }
            }
        };
    }


    //region java aliases
    public static <T> boolean equalIgnoreCase(String o1, String o2) {
        return Fu.coalesce(o1, o2).map($ -> $.equalsIgnoreCase(o1) && $.equalsIgnoreCase(o2)).orElse(true);
    }

    public static <T> boolean equal(T o1, T o2) {
        return Objects.equals(o1, o2);
    }

    public static <T> boolean notEqual(T o1, T o2) {
        return !Objects.equals(o1, o2);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable> int compare(T o1, T o2) {
        return Objects.compare(o1, o2, naturalOrder());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable> int compareReverse(T o1, T o2) {
        return Objects.compare(o1, o2, reverseOrder());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable> int compareN(T o1, T o2) {
        return Objects.compare(o1, o2, nullsFirst(naturalOrder()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable> int compareReverseN(T o1, T o2) {
        return Objects.compare(o1, o2, nullsLast(reverseOrder()));
    }
    //endregion

    public static <T> Predicate<T> notNull() {
        return Objects::nonNull;
    }

    //region Private
    private Fu() {}
    //endregion
}
