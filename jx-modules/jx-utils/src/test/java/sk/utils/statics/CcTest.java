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

import org.junit.Before;
import org.junit.Test;
import sk.test.MockitoTest;
import sk.utils.functional.O;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static sk.utils.statics.Cc.*;

public class CcTest extends MockitoTest {

    private List<Integer> l;
    private Map<Integer, String> m;

    @Before
    public void setUpTest() {
        l = new ArrayList<Integer>() {
            {
                add(1);
                add(2);
                add(3);
                add(4);
                add(5);
            }
        };
        m = new HashMap<Integer, String>() {
            {
                put(1, "1");
                put(2, "2");
                put(3, "3");
            }
        };
    }

    @Test
    public void firstIndexTest() {
        assertEquals(firstIndex(l, $ -> $ == 3), 2);
    }

    @Test
    public void addTest() {
        assertEquals(add(l, 6).get(5).intValue(), 6);
    }

    @Test
    public void removeTest() {
        assertEquals(remove(l, 1).get(0).intValue(), 2);
    }

    @Test
    public void addAllTest() {
        List<Integer> vals = addAll(l, l, l);
        assertEquals(join("", vals), "12345123451234512345");
    }

    @Test
    public void removeAllTest() {
        List<Integer> vals = removeAll(l, l(1, 2, 3));
        assertEquals(join("", vals), "45");
    }

    @Test
    public void retainAllTest() {
        List<Integer> vals = retainAll(l, l(2, 3));
        assertEquals(join("", vals), "23");
    }

    @Test
    public void orderingTest() {
        assertEquals(joinMap("", ":", ordering(l)), "1:02:13:24:35:4");
    }

    @Test
    public void sortTest() {
        assertNotEquals(join(shuffle(l, new Random())), "12345");
        assertEquals(join("", sort(l)), "12345");
    }

    @Test
    public void mapTest() {
        equalRaw(map(l, $ -> $ + 1), "23456");
    }

    @Test
    public void lastTest() {
        assertEquals(last(l), O.of(5));
    }

    @Test
    public void firstTest() {
        assertEquals(first(l), O.of(1));
    }

    @Test
    public void listTest() {
        assertEquals(join("", list(() -> new Iterator<Integer>() {
            int counter;

            @Override
            public boolean hasNext() {
                return counter < 5;
            }

            @Override
            public Integer next() {
                return counter++;
            }
        })), "01234");
    }

    @Test
    public void reverseTest() {
        equalRaw(reverse(l), "54321");
    }

    @Test
    public void fillTest() {
        equalRaw(fill(5, () -> "a"), "aaaaa");
    }

    @Test
    public void fillFunTest() {
        equalRaw(fillFun(5, i -> i), "01234");
    }

    @Test
    public void getOrDefaultTest() {
        assertEquals(getOrDefault(l, 10, 66).intValue(), 66);
    }

    @Test
    public void filterTest() {
        equalRaw(filter(l, $ -> $ < 3), "12");
    }

    @Test
    public void computeTest() {
        assertEquals(Cc.compute(m, 1, (k, v) -> (k + 1) + "", () -> "x"), "2");
        assertEquals(Cc.compute(m, 5, (k, v) -> (k + 1) + "", () -> "x"), "x");
    }

    @Test
    public void computeAndApplyTest() {
        assertEquals(Cc.computeAndApply(m, 1, (k, v) -> v + 1, () -> "x"), "11");
        assertEquals(Cc.computeAndApply(m, 5, (k, v) -> v + 1, () -> "x"), "x1");
    }

    @Test
    public void putAllTest() {
        equalRaw(putAll(m, m), "112233");
        equalRaw(putAll(m, m.entrySet().stream().collect(toM(e -> e.getKey() * 4, Map.Entry::getValue))),
                "1122334182123");
    }

    @Test
    public void putTest() {
        equalRaw(put(m, 6, "6"), "11223366");
    }

    @Test
    public void groupByTest() {
        Map<Boolean, List<Integer>> mt = groupBy(l, $ -> $ > 3);
        equalRaw(sort(mt.get(true)), "45");
        equalRaw(sort(mt.get(false)), "123");
    }

    @Test
    public void mapByTest() {
        equalRaw(mapBy(l, $ -> "" + $), "1122334455");
    }

    @Test
    public void forEachWithIndexTest() {
        eachWithIndex(l, (v, i) -> assertEquals((int) v, i + 1));
    }

    @Test
    public void forEachWithEachTest() {
        List<Integer> newL = l();
        eachWithEach(l(1, 2), l(3, 4), (i1, i2) -> newL.add(i1 * i2));
        equalRaw(sort(newL), "3468");
    }

    @Test
    public void forEachSyncTest() {
        eachSync(l, l(5, 4, 3, 2, 1), (a, b) -> assertEquals(6 - a, b.intValue()));
    }

    @Test
    public void mapEachWithIndexTest() {
        equalRaw(list(mapEachWithIndex(l, (k, i) -> k + "" + (i + 1))), "1122334455");
    }

    @Test
    public void mapSyncTest() {
        equalRaw(mapSync(l, l, (k1, k2, il) -> k1 + "" + k2), "1122334455");
    }

    @Test
    public void mapEachWithEachTest() {
        equalRaw(mapEachWithEach(l(1, 2), l(3, 4), (i1, i2) -> i1 * i2), "3468");
    }

    @Test
    public void strTest() {
        equalRaw(str(l, $ -> $ * $ + ""), "1491625");
    }

    @Test
    public void addStreamTest() {
        assertEquals(join("", addStream(l.stream(), 6, 7).sorted()), "1234567");
    }

    @Test
    public void getXLastElementsAndDeleteThemFromOriginalSafeTest() {
        List<Integer> toTest = getXLastElementsAndDeleteThemFromOriginalSafe(l, 0);
        equalRaw(toTest, "");
        equalRaw(l, "12345");

        toTest = getXLastElementsAndDeleteThemFromOriginalSafe(l, 1);
        equalRaw(toTest, "5");
        equalRaw(l, "1234");

        toTest = getXLastElementsAndDeleteThemFromOriginalSafe(l, 2);
        equalRaw(toTest, "43");
        equalRaw(l, "12");

        toTest = getXLastElementsAndDeleteThemFromOriginalSafe(l, 10);
        equalRaw(toTest, "21");
        equalRaw(l, "");

        toTest = getXLastElementsAndDeleteThemFromOriginalSafe(l, 10);
        equalRaw(toTest, "");
        equalRaw(l, "");
    }

    //region Equal Raw
    private <T> void equalRaw(Iterable<T> l1, String l2) {
        assertEquals(join("", l1), l2);
    }

    private <K, V> void equalRaw(Map<K, V> l1, String l2) {
        assertEquals(joinMap("", "", new TreeMap<>(l1)), l2);
    }
    //endregion
}
