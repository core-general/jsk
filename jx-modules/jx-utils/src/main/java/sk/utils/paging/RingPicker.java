package sk.utils.paging;

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
import sk.utils.functional.P1;
import sk.utils.functional.R;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;

import java.util.Iterator;
import java.util.List;

import static sk.utils.functional.O.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public class RingPicker<T> implements Iterable<T> {
    private List<T> lst;
    private int index;


    public static <X> RingPicker<X> create(List<X> l, int index) {
        return new RingPicker<>(l, index);
    }

    public static <X> RingPicker<X> createAndRotate(List<X> l, int index) {
        return new RingPicker<>(l, index > 0 ? index % l.size() : 0);
    }

    public static <X> RingPicker<X> create(List<X> l) {
        return new RingPicker<>(l);
    }


    private RingPicker(List<T> lst) {
        this(lst, 0);
    }

    private RingPicker(List<T> lst, int index) {
        this.lst = lst;
        this.index = index;
    }

    private RingPicker(RingPicker<T> other) {
        this.lst = other.lst;
        this.index = other.index;
    }

    //region Next and prev
    public boolean hasNext() {
        return !(lst.size() - 1 == index || lst.isEmpty());
    }

    public RingPicker<T> nextRotate() {
        if (reindexAndCheck()) {
            index = (index + 1) % lst.size();
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public O<RingPicker<T>> nextStop() {
        return hasNext() ? of(this.nextRotate()) : empty();
    }

    public boolean nextRotate(P1<T> predicate) {
        return withPredicate(this::nextRotate, predicate);
    }

    public boolean hasPrev() {
        return !(index == 0 || lst.isEmpty());
    }

    public RingPicker<T> prevRotate() {
        if (reindexAndCheck()) {
            index = index == 0 ? lst.size() - 1 : index - 1;
        }
        return this;
    }

    public boolean prevRotate(P1<T> predicate) {
        return withPredicate(this::prevRotate, predicate);
    }

    public O<RingPicker<T>> prevStop() {
        return hasPrev() ? of(this.prevRotate()) : empty();
    }
    //endregion


    public RingPicker<T> remove() {
        if (reindexAndCheck()) {
            lst.remove(index);
            prevRotate();
        }
        return this;
    }

    public RingPicker<T> set(T o) {
        if (reindexAndCheck()) {
            lst.set(index, o);
        }
        return this;
    }

    public RingPicker<T> add(T o) {
        reindexAndCheck();
        lst.add(index, o);
        return this;
    }

    public O<T> get() {
        return reindexAndCheck() ? ofNullable(lst.get(index)) : empty();
    }

    public RingPicker<T> setIndex(int index) {
        this.index = index;
        return this;
    }

    public int getIndex() {
        return reindexAndCheck() ? index : -1;
    }


    public boolean isEmpty() {
        return lst.isEmpty();
    }


    @Override
    public Iterator<T> iterator() {
        X1<Boolean> first = X.x(true);
        RingPicker<T> newCp = new RingPicker<>(lst);
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return RingPicker.this.hasNext();
            }

            @Override
            public T next() {
                if (first.get()) {
                    first.set(false);
                    return newCp.get().get();
                } else {
                    return newCp.nextRotate().get().get();
                }

            }
        };
    }

    private boolean reindexAndCheck() {
        if (index > lst.size()) {
            index = Math.max(lst.size() - 1, 0);
        }
        return lst.size() > 0;
    }

    private boolean withPredicate(R next, P1<T> predicate) {
        int curInd = index;
        next.run();
        O<T> t = get();
        if (t.isEmpty()) {
            return false;
        }
        if (predicate.test(t.get())) {
            return true;
        }
        while (index != curInd) {
            next.run();
            t = get();
            if (predicate.test(t.orElse(null))) {
                return true;
            }
        }
        return false;
    }
}
