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

import org.jetbrains.annotations.NotNull;
import sk.utils.functional.*;
import sk.utils.tuples.X2;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.*;

import static java.util.stream.Collectors.*;
import static sk.utils.functional.O.*;


@SuppressWarnings({"WeakerAccess", "unused"})
public final class Cc {
    //region String joiners
    public static <A, B extends Iterable<A>> List<String> str(B b) {
        return str(b, Objects::toString);
    }

    public static <A, B extends Iterable<A>> List<String> str(B b, F1S<A> o2s) {
        return stream(b).map(o2s).collect(toList());
    }

    public static <A, B extends Iterable<A>> String join(B a) {
        return join(",", a);
    }

    public static <A, B extends Iterable<A>> String join(String del, B b) {
        return join(del, b, Objects::toString);
    }

    public static <A, B extends Iterable<A>> String join(B b, F1S<A> o2s) {
        return join(",", b, o2s);
    }

    public static <A, B extends Iterable<A>> String join(String del, B b, F1S<A> o2s) {
        return String.join(del, str(b, o2s));
    }

    public static <K, V, B extends Map<K, V>> String
    joinMap(B b) {
        return joinMap(b, (k, v) -> k.toString(), (k, v) -> v.toString());
    }

    public static <K, V, B extends Map<K, V>> String
    joinMap(B b, F2S<K, V> o2sKey, F2S<K, V> o2sVal) {
        return joinMap(", ", ":", b, o2sKey, o2sVal);
    }

    public static <K, V, B extends Map<K, V>> String
    joinMap(String iDel, String keyDel, B b) {
        return joinMap(iDel, keyDel, b, (k, v) -> k.toString(), (k, v) -> v.toString());
    }

    public static <K, V, B extends Map<K, V>> String
    joinMap(String iDel, String keyDel,
            B b, F2S<K, V> toStringerKey, F2S<K, V> toStringerValue) {
        return join(iDel, b.entrySet(),
                o -> toStringerKey.apply(o.getKey(), o.getValue()) + keyDel +
                        toStringerValue.apply(o.getKey(), o.getValue()));
    }

    public static <A, B extends Stream<A>> String join(B b) {
        return join(", ", b);
    }

    public static <A, B extends Stream<A>> String join(String del, B b) {
        return join(del, b, Objects::toString);
    }

    public static <A, B extends Stream<A>> String join(B b, F1S<A> o2s) {
        return join(", ", b, o2s);
    }

    public static <A, B extends Stream<A>> String join(String del, B b, F1S<A> o2s) {
        return String.join(del, str(b.collect(toList()), o2s));
    }

    //endregion

    //region iteration


    public static <A> void eachWithIndex(Iterable<A> nexter, C2<A, Integer> c) {
        int i = 0;
        for (A next : nexter) {
            c.accept(next, i++);
        }
    }

    public static <A, B> void eachWithEach(Iterable<A> t1, Iterable<B> t2, C2<A, B> c) {
        for (A a : t1) {
            for (B b : t2) {
                c.accept(a, b);
            }
        }
    }

    public static <A, B> void eachSync(Iterable<A> t1, Iterable<B> t2, C2<A, B> c) {
        eachSync(t1, t2, (a, b, i) -> c.accept(a, b));
    }

    public static <A, B> void eachSync(Iterable<A> t1, Iterable<B> t2, C3<A, B, Integer> cwi) {
        Iterator<A> it1 = t1.iterator();
        Iterator<B> it2 = t2.iterator();
        int i = 0;
        while (it1.hasNext() || it2.hasNext()) {
            A a = (it1.hasNext()) ? it1.next() : null;
            B b = (it2.hasNext()) ? it2.next() : null;
            cwi.accept(a, b, i++);
        }
    }

    public static <A, B> List<B> mapEachWithIndex(Iterable<A> nexter, F2<A, Integer, B> f) {
        int i = 0;
        List<B> toRet = l();
        for (A next : nexter) {
            toRet.add(f.apply(next, i++));
        }
        return toRet;
    }

    public static <A, B, C> List<C> mapSync(Iterable<A> t1, Iterable<B> t2, F3<A, B, Integer, C> c) {
        List<C> result = l();
        eachSync(t1, t2, (a, b, i) -> result.add(c.apply(a, b, i)));
        return result;
    }

    public static <A, B, C> List<C> mapEachWithEach(Iterable<A> t1, Iterable<B> t2,
            F2<A, B, C> c) {
        List<C> result = l();
        eachWithEach(t1, t2, (a, b) -> result.add(c.apply(a, b)));
        return result;
    }
    //endregion

    //region Streams
    public static <T> Stream<T> stream(Iterable<T> nexter) {
        return StreamSupport.stream(nexter.spliterator(), false);
    }

    @SafeVarargs
    public static <T> Stream<T> stream(T... arr) {
        return Stream.of(arr);
    }

    public static <T> Stream<T> addStream(Stream<T> data, Stream<T> data2) {
        return Stream.concat(data, data2);
    }

    @SuppressWarnings("unchecked")
    public static <T> Stream<T> addStream(Stream<T> stream, T... data) {
        return Stream.concat(stream, Stream.of(data));
    }

    public static <T> Stream<T> addStream(Stream<T> stream, Collection<T> data) {
        return Stream.concat(stream, data.stream());
    }

    public static <T> Collector<T, ?, List<T>> toL() {
        return Collectors.toList();
    }

    public static <T> Collector<T, ?, Set<T>> toS() {
        return Collectors.toSet();
    }

    public static <T, K, U>
    Collector<T, ?, Map<K, U>> toM(F1<? super T, ? extends K> keyMapper, F1<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper);
    }

    public static <T, K, U>
    Collector<T, ?, Map<K, U>> toM(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return Collectors.toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }

    public static <A, B>
    Collector<X2<A, B>, ?, Map<A, B>> toMX2() {
        return Collectors.toMap($ -> $.i1, $ -> $.i2);
    }

    public static <A, B>
    Collector<Map.Entry<A, B>, ?, Map<A, B>> toMEntry() {
        return Collectors.toMap($ -> $.getKey(), $ -> $.getValue());
    }

    public static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {throw new IllegalStateException(String.format("Duplicate key %s", u));};
    }
    //endregion

    //region Enumeration
    @SuppressWarnings("Convert2Lambda")
    public static <E> Iterable<E> enumerableToIterable(final Enumeration<E> e) {
        Objects.requireNonNull(e);
        return new Iterable<E>() {
            @NotNull
            @Override
            public Iterator<E> iterator() {
                return new Iterator<E>() {
                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }

                    public E next() {
                        return e.nextElement();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static <E> List<E> l(final Enumeration<E> data) {
        return list(enumerableToIterable(data));
    }
    //endregion

    //region Collections
    public static <A> int firstIndex(Iterable<A> nexter, P1<A> p) {
        int i = 0;
        for (A a : nexter) {
            if (p.test(a)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static <T, A extends Collection<T>> A add(A col, T e) {
        col.add(e);
        return col;
    }

    public static <T, A extends Collection<T>> A remove(A col, T e) {
        col.remove(e);
        return col;
    }

    @SafeVarargs
    public static <T, A extends Collection<T>, B extends Collection<T>> A addAll(A col1, B... cs) {
        for (B col : cs) {
            col1.addAll(col);
        }
        return col1;
    }

    public static <T, A extends Collection<T>, B extends Collection<T>> A removeAll(A col1, B col2) {
        col1.removeAll(col2);
        return col1;
    }

    public static <T, A extends Collection<T>, B extends Collection<T>> A retainAll(A col1, B col2) {
        col1.retainAll(col2);
        return col1;
    }

    public static <T> Collection<T> addAll(Collection<T> addTo, Iterator<? extends T> iterator) {
        while (iterator.hasNext()) {
            addTo.add(iterator.next());
        }
        return addTo;
    }


    public static <T, A extends Collection<T>> O<T> find(A collection, P1<T> predicate) {
        return of(collection.stream().filter(predicate).findAny());
    }

    public static <T, A extends Collection<T>> Map<Long, List<T>>
    splitCollectionRandomly(A collection, int aproxSizeOfPartition, F0<Long> randomSequence) {
        int size = collection.size();
        int mod = Math.max(size / aproxSizeOfPartition, 1);
        return collection.stream().collect(groupingBy($ -> randomSequence.apply() % mod));
    }
    //endregion

    //region Lists
    public static <T> Map<T, Integer> ordering(List<T> in) {
        Map<T, Integer> toRet = m();
        eachWithIndex(in, toRet::put);
        return toRet;
    }

    public static <T> Comparator<T> orderingComparator(Map<T, Integer> ordering) {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                final Integer order1 = ordering.get(o1);
                final Integer order2 = ordering.get(o2);
                return (order1 != null && order2 != null
                        ? order1.compareTo(order2)
                        : order1 == null && order2 == null
                          ? 0
                          : order1 != null && order2 == null
                            ? -1
                            : order1 == null && order2 != null
                              ? 1
                              : 0);
            }
        };
    }

    public static <T extends Enum<T>> EnumMap<T, Integer> orderingEnum(Class<T> cls, List<T> in) {
        EnumMap<T, Integer> toRet = em(cls);
        eachWithIndex(in, toRet::put);
        return toRet;
    }

    public static <T, A extends List<T>> A sort(A col1, Comparator<T> comp) {
        col1.sort(comp);
        return col1;
    }

    public static <T extends Comparable<T>, A extends List<T>> A sort(A col1) {
        col1.sort(Comparable::compareTo);
        return col1;
    }

    public static <A, B> List<B> map(List<A> in, F1<A, B> f) {
        return in.stream().map(f).collect(toList());
    }

    public static <A> O<A> last(List<A> in) {
        return in.size() == 0 ? empty() : ofNullable(in.get(in.size() - 1));
    }

    public static <A> O<A> first(List<A> in) {
        return in.size() == 0 ? empty() : ofNullable(in.get(0));
    }

    public static <A> O<A> getAt(List<A> in, int index) {
        return index < 0 || index > in.size() - 1 ? empty() : ofNullable(in.get(index));
    }

    public static <A> List<A> list(Iterable<? extends A> nexter) {
        return stream(nexter).collect(toList());
    }

    public static <A> Set<A> set(Iterable<? extends A> nexter) {
        return stream(nexter).collect(toSet());
    }

    public static <A> List<A> shuffle(List<A> data, Random rand) {
        Collections.shuffle(data, rand);
        return data;
    }

    public static <A> List<A> reverse(List<A> uris) {
        Collections.reverse(uris);
        return uris;
    }

    public static <T> List<T> fill(int listCount, T defaultValue) {
        return fill(listCount, () -> defaultValue);
    }

    public static <T> List<T> filter(List<T> l, P1<T> p) {
        return l.stream().filter(p).collect(toL());
    }

    public static <T> List<T> fill(int listCount, F0<T> defaultValue) {
        return IntStream.range(0, listCount)
                .mapToObj($ -> defaultValue.get())
                .collect(toL());
    }

    public static <T> List<T> fillFun(int listCount, @NotNull F1<Integer, T> defaultValue) {
        return IntStream.range(0, listCount)
                .mapToObj(defaultValue::apply)
                .collect(toL());
    }

    public static <T> List<T> getXLastElementsAndDeleteThemFromOriginalSafe(List<T> originalWillBeMutated, int X) {
        List<T> toRet = Cc.l();
        for (int i = originalWillBeMutated.size() - 1; i >= 0 && toRet.size() < X; i--) {
            toRet.add(originalWillBeMutated.get(i));
            originalWillBeMutated.remove(i);
        }
        return toRet;
    }

    public static <T> T getOrDefault(List<T> in, int index, T _default) {
        return index < in.size() ? in.get(index) : _default;
    }

    //endregion

    //region Maps
    public static <K, V> Map<K, V> filter(Map<K, V> map, P2<K, V> filter) {
        return map.entrySet().stream()
                .filter(kvEntry -> filter.test(kvEntry.getKey(), kvEntry.getValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <A, B> B compute(Map<A, B> map, A key, F2<A, B, B> remapping, F0<B> startingValue) {
        return map.compute(key, (a, b) -> b == null ? startingValue.get() : remapping.apply(a, b));
    }

    public static <A, B> B computeAndApply(Map<A, B> map, A key, F2<A, B, B> remap, F0<B> startingValueThenRemap) {
        return map.compute(key, (a, b) -> b == null ? remap.apply(a, startingValueThenRemap.get()) : remap.apply(a, b));
    }

    public static <T, X, A extends Map<T, X>, B extends Map<T, X>> A putAll(A collector, B map) {
        collector.putAll(map);
        return collector;
    }

    public static <T, X, A extends Map<T, X>> A put(A map, T key, X val) {
        map.put(key, val);
        return map;
    }

    public static <T, X, A extends Map<T, X>> A put(A map, T key, X val, T key1, X val1) {
        map.put(key, val);
        map.put(key1, val1);
        return map;
    }

    public static <T, X, A extends Map<T, X>> A put(A map, T key, X val, T key1, X val1, T key2, X val2) {
        map.put(key, val);
        map.put(key1, val1);
        map.put(key2, val2);
        return map;
    }

    public static <T, X, A extends Map<T, X>> A put(A map, T key, X val, T key1, X val1, T key2, X val2, T key3, X val3) {
        map.put(key, val);
        map.put(key1, val1);
        map.put(key2, val2);
        map.put(key3, val3);
        return map;
    }

    public static <T, X, A extends Map<T, X>> A put(A map, T key, X val, T key1, X val1, T key2, X val2, T key3, X val3, T key4,
            X val4) {
        map.put(key, val);
        map.put(key1, val1);
        map.put(key2, val2);
        map.put(key3, val3);
        map.put(key4, val4);
        return map;
    }

    public static <T, X, A extends Map<T, X>> A put(A map, T key, X val, T key1, X val1, T key2, X val2, T key3, X val3, T key4,
            X val4, T key5, X val5) {
        map.put(key, val);
        map.put(key1, val1);
        map.put(key2, val2);
        map.put(key3, val3);
        map.put(key4, val4);
        map.put(key5, val5);
        return map;
    }

    public static <T, X, A extends Map<T, X>> A put(A map, T key, X val, T key1, X val1, T key2, X val2, T key3, X val3, T key4,
            X val4, T key5, X val5, T key6, X val6) {
        map.put(key, val);
        map.put(key1, val1);
        map.put(key2, val2);
        map.put(key3, val3);
        map.put(key4, val4);
        map.put(key5, val5);
        map.put(key6, val6);
        return map;
    }

    public static <T, A extends Map<T, ?>> A remove(A map, T e) {
        map.remove(e);
        return map;
    }

    public static <A, B, C extends Collection<B>> Map<A, List<B>> groupBy(C input, F1<B, A> grouper) {
        return input.stream().collect(groupingBy(grouper));
    }

    public static <A, B, C extends Collection<B>> Map<A, B> mapBy(C input, F1<B, A> grouper) {
        return input.stream().collect(toMap(grouper, $ -> $));
    }
    //endregion

    //region Creators
    public static <T> List<T> lEmpty() {
        return Collections.emptyList();
    }

    public static <A, B> Map<A, B> mEmpty() {
        return Collections.emptyMap();
    }

    public static <T> Set<T> sEmpty() {return Collections.emptySet();}

    @SafeVarargs
    public static <T> List<T> l(T... ts) {
        return new ArrayList<>(Arrays.asList(ts));
    }

    @SafeVarargs
    public static <T> T[] a(T... ts) {
        return ts;
    }

    @SafeVarargs
    public static <T> Queue<T> q(T... ts) {
        return new LinkedList<>(Arrays.asList(ts));
    }

    @SafeVarargs
    public static <T> Set<T> s(T... ts) {
        return new HashSet<>(Arrays.asList(ts));
    }

    @SafeVarargs
    public static <T> TreeSet<T> ts(T... ts) {
        return new TreeSet<>(Arrays.asList(ts));
    }

    public static <K, V> Map<K, V> m() {
        return new HashMap<>();
    }

    private static <K, V> void putToMapWithCheck(Map<K, V> map, K k, V v) {
        if (!map.containsKey(k)) {
            map.put(k, v);
        } else {
            throw new RuntimeException(String.format("Key %s is already in map", k));
        }
    }

    public static <K, V> Map<K, V> m(K k, V v) {
        Map<K, V> ret = m();
        putToMapWithCheck(ret, k, v);
        return ret;
    }

    public static <K, V> Map<K, V> m(K k, V v, K k1, V v1) {
        Map<K, V> ret = m(k, v);
        putToMapWithCheck(ret, k1, v1);
        return ret;
    }

    public static <K, V> Map<K, V> m(K k, V v, K k1, V v1, K k2, V v2) {
        Map<K, V> ret = m(k, v, k1, v1);
        putToMapWithCheck(ret, k2, v2);
        return ret;
    }

    public static <K, V> Map<K, V> m(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2);
        putToMapWithCheck(ret, k3, v3);
        return ret;
    }

    public static <K, V> Map<K, V> m(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3);
        putToMapWithCheck(ret, k4, v4);
        return ret;
    }

    public static <K, V> Map<K, V> m(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4);
        putToMapWithCheck(ret, k5, v5);
        return ret;
    }

    public static <K, V> Map<K, V> m(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        putToMapWithCheck(ret, k6, v6);
        return ret;
    }

    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        putToMapWithCheck(ret, k7, v7);
        return ret;
    }

    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        putToMapWithCheck(ret, k8, v8);
        return ret;
    }

    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
            K k9, V v9) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        putToMapWithCheck(ret, k9, v9);
        return ret;
    }

    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
            K k9, V v9, K k10, V v10) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
        putToMapWithCheck(ret, k10, v10);
        return ret;
    }

    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
            K k9, V v9, K k10, V v10, K k11, V v11) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
        putToMapWithCheck(ret, k11, v11);
        return ret;
    }

    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
            K k9, V v9, K k10, V v10, K k11, V v11, K k12, V v12) {
        Map<K, V> ret = m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10, k11, v11);
        putToMapWithCheck(ret, k12, v12);
        return ret;
    }


    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
            K k9, V v9, K k10, V v10, K k11, V v11, K k12, V v12, K k13, V v13) {
        Map<K, V> ret =
                m(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10, k11, v11, k12, v12);
        putToMapWithCheck(ret, k13, v13);
        return ret;
    }

    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
            K k9, V v9, K k10, V v10, K k11, V v11, K k12, V v12, K k13, V v13, K k14, V v14) {
        Map<K, V> ret = m(k, v, k1, v1,
                k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10, k11, v11, k12, v12, k13, v13);
        putToMapWithCheck(ret, k14, v14);
        return ret;
    }


    public static <K, V> Map<K, V> m(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
            K k9, V v9, K k10, V v10, K k11, V v11, K k12, V v12, K k13, V v13, K k14, V v14, K k15, V v15) {
        Map<K, V> ret = m(k, v, k1, v1,
                k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10, k11, v11, k12, v12, k13, v13, k14, v14);
        putToMapWithCheck(ret, k15, v15);
        return ret;
    }


    public static <K extends Comparable, V> TreeMap<K, V> tm() {
        return new TreeMap<>();
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(K k, V v) {
        TreeMap<K, V> ret = tm();
        putToMapWithCheck(ret, k, v);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(K k, V v, K k1, V v1) {
        TreeMap<K, V> ret = tm(k, v);
        putToMapWithCheck(ret, k1, v1);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(K k, V v, K k1, V v1, K k2, V v2) {
        TreeMap<K, V> ret = tm(k, v, k1, v1);
        putToMapWithCheck(ret, k2, v2);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3) {
        TreeMap<K, V> ret = tm(k, v, k1, v1, k2, v2);
        putToMapWithCheck(ret, k3, v3);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        TreeMap<K, V> ret = tm(k, v, k1, v1, k2, v2, k3, v3);
        putToMapWithCheck(ret, k4, v4);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5,
            V v5) {
        TreeMap<K, V> ret = tm(k, v, k1, v1, k2, v2, k3, v3, k4, v4);
        putToMapWithCheck(ret, k5, v5);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
            K k6, V v6) {
        TreeMap<K, V> ret = tm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        putToMapWithCheck(ret, k6, v6);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        TreeMap<K, V> ret = tm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        putToMapWithCheck(ret, k7, v7);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        TreeMap<K, V> ret = tm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        putToMapWithCheck(ret, k8, v8);
        return ret;
    }

    public static <K extends Comparable, V> TreeMap<K, V> tm(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9,
            V v9) {
        TreeMap<K, V> ret = tm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        putToMapWithCheck(ret, k9, v9);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls) {
        return new EnumMap<K, V>(cls);
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v) {
        EnumMap<K, V> ret = em(cls);
        putToMapWithCheck(ret, k, v);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1) {
        EnumMap<K, V> ret = em(cls, k, v);
        putToMapWithCheck(ret, k1, v1);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1);
        putToMapWithCheck(ret, k2, v2);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2, K k3, V v3) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1, k2, v2);
        putToMapWithCheck(ret, k3, v3);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4,
            V v4) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1, k2, v2, k3, v3);
        putToMapWithCheck(ret, k4, v4);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5,
            V v5) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1, k2, v2, k3, v3, k4, v4);
        putToMapWithCheck(ret, k5, v5);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5,
            K k6, V v6) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        putToMapWithCheck(ret, k6, v6);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        putToMapWithCheck(ret, k7, v7);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        putToMapWithCheck(ret, k8, v8);
        return ret;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> em(Class<K> cls, K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9,
            V v9) {
        EnumMap<K, V> ret = em(cls, k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        putToMapWithCheck(ret, k9, v9);
        return ret;
    }


    public static <K, V> LinkedHashMap<K, V> lhm() {
        return new LinkedHashMap<>();
    }

    public static <K, V> LinkedHashMap<K, V> lhm(K k, V v) {
        LinkedHashMap<K, V> ret = lhm();
        putToMapWithCheck(ret, k, v);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(K k, V v, K k1, V v1) {
        LinkedHashMap<K, V> ret = lhm(k, v);
        putToMapWithCheck(ret, k1, v1);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(K k, V v, K k1, V v1, K k2, V v2) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1);
        putToMapWithCheck(ret, k2, v2);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1, k2, v2);
        putToMapWithCheck(ret, k3, v3);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1, k2, v2, k3, v3);
        putToMapWithCheck(ret, k4, v4);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1, k2, v2, k3, v3, k4, v4);
        putToMapWithCheck(ret, k5, v5);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6,
            V v6) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        putToMapWithCheck(ret, k6, v6);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        putToMapWithCheck(ret, k7, v7);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        putToMapWithCheck(ret, k8, v8);
        return ret;
    }

    public static <K, V> LinkedHashMap<K, V> lhm(
            K k, V v, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9,
            V v9) {
        LinkedHashMap<K, V> ret = lhm(k, v, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        putToMapWithCheck(ret, k9, v9);
        return ret;
    }

    //endregion

    //region Private
    private Cc() {
    }

    //endregion
}
