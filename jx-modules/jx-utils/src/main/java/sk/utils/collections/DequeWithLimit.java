package sk.utils.collections;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DequeWithLimit<T> {
    int maxSize;
    ArrayDeque<T> deque;

    public DequeWithLimit(int maxSize) {
        this.maxSize = maxSize;
        deque = new ArrayDeque<>();
    }

    public void addFirst(@NotNull T t) {
        deque.addFirst(t);
        while (deque.size() > maxSize) {
            deque.removeLast();
        }
    }

    public void addLast(@NotNull T t) {
        deque.addLast(t);
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    public boolean add(@NotNull T t) {
        addLast(t);
        return true;
    }

    public void push(@NotNull T t) {
        addFirst(t);
    }

    public boolean offerFirst(@NotNull T t) {return deque.offerFirst(t);}

    public boolean offerLast(@NotNull T t) {return deque.offerLast(t);}

    public T removeFirst() {return deque.removeFirst();}

    public T removeLast() {return deque.removeLast();}

    public T pollFirst() {return deque.pollFirst();}

    public T pollLast() {return deque.pollLast();}

    public T getFirst() {return deque.getFirst();}

    public T getLast() {return deque.getLast();}

    public T peekFirst() {return deque.peekFirst();}

    public T peekLast() {return deque.peekLast();}

    public boolean removeFirstOccurrence(Object o) {return deque.removeFirstOccurrence(o);}

    public boolean removeLastOccurrence(Object o) {return deque.removeLastOccurrence(o);}

    public boolean offer(@NotNull T t) {return deque.offer(t);}

    public T remove() {return deque.remove();}

    public T poll() {return deque.poll();}

    public T element() {return deque.element();}

    public T peek() {return deque.peek();}

    public T pop() {return deque.pop();}

    public int size() {return deque.size();}

    public boolean isEmpty() {return deque.isEmpty();}

    public Iterator<T> iterator() {return deque.iterator();}

    public Iterator<T> descendingIterator() {return deque.descendingIterator();}

    public boolean contains(Object o) {return deque.contains(o);}

    public boolean remove(Object o) {return deque.remove(o);}

    public void clear() {deque.clear();}

    public Object[] toArray() {return deque.toArray();}

    public <T1> T1[] toArray(T1[] a) {return deque.toArray(a);}

    public Spliterator<T> spliterator() {return deque.spliterator();}

    public boolean containsAll(Collection<?> c) {return deque.containsAll(c);}

    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            add(t);
        }
        return true;
    }

    public boolean removeAll(Collection<?> c) {return deque.removeAll(c);}

    public boolean retainAll(Collection<?> c) {return deque.retainAll(c);}

    public boolean removeIf(Predicate<? super T> filter) {return deque.removeIf(filter);}

    public Stream<T> stream() {return deque.stream();}

    public Stream<T> parallelStream() {return deque.parallelStream();}

    public void forEach(Consumer<? super T> action) {deque.forEach(action);}
}
