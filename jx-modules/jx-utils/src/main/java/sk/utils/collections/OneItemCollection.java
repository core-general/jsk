package sk.utils.collections;

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


import sk.utils.statics.Fu;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static sk.utils.statics.Fu.equal;


public class OneItemCollection<T> implements Collection<T> {
    private T item;

    public OneItemCollection(T item) {
        this.item = item;
    }

    public OneItemCollection() {
        this.item = null;
    }

    @Override
    public int size() {
        return item == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return item == null;
    }

    @Override
    public boolean contains(Object o) {
        return equal(item, o);
    }


    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index == 0 && item != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                } else {
                    index++;
                    return item;
                }
            }
        };
    }


    @Override
    public Object[] toArray() {
        return item == null ? new Object[0] : new Object[]{item};
    }


    @Override
    public <T1> T1[] toArray(T1[] a) {
        if (a.length == 0) {
            return (T1[]) (item == null ? new Object[]{} : new Object[]{item});
        } else {
            a[0] = (T1) item;
            return a;
        }
    }

    @Override
    public boolean add(T t) {
        item = t;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (equal(item, o)) {
            item = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c.size() == 0 && size() == 0) {
            return true;
        }
        return c.stream().allMatch($ -> equal(item, $));
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            item = t;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        for (Object item : c) {
            if (equal(item, this.item)) {
                this.item = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean anyMatch = c.stream().anyMatch($ -> Fu.equal($, item));
        if (!anyMatch) {
            item = null;
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        item = null;
    }

    public boolean equals(final Object o) {
        if (o == this) { return true; }
        if (!(o instanceof OneItemCollection)) { return false; }
        final OneItemCollection<?> other = (OneItemCollection<?>) o;
        if (!other.canEqual((Object) this)) { return false; }
        final Object this$item = this.item;
        final Object other$item = other.item;
        if (this$item == null ? other$item != null : !this$item.equals(other$item)) { return false; }
        return true;
    }

    protected boolean canEqual(final Object other) {return other instanceof OneItemCollection;}

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $item = this.item;
        result = result * PRIME + ($item == null ? 43 : $item.hashCode());
        return result;
    }
}
