package sk.utils.collections.cluster_sorter;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.Test;
import sk.utils.collections.cluster_sorter.model.JskCsList;
import sk.utils.statics.Cc;
import sk.utils.statics.Ma;
import sk.utils.tuples.X;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class JskCsTest {
    private static final String E/*XPANDABLE*/ = "💼";
    private static final Comparator COMP = new Comparator();

    @Test
    public void test_sameids_throw_exception() {
        assertThrows(RuntimeException.class, () -> JskCs.simple(Cc.l(
                new JskTestSource(0, 2),
                new JskTestSource(0, 5)
        ), COMP));
    }

    @Test
    public void test_real_life_with_batch() {
        //real life simulation with batch
        final JskCs<Integer, String> cs = JskCs
                .batch(IntStream.range(0, 100).mapToObj($ -> new JskTestSource($, 20)).toList(), COMP, new JskTestBatch());
        makeRealLifeExperimentAndCheck(cs);
        assertEquals(4/*!!!MUCH LESS WITH BATCH !!!*/, cs.getExpandsDone());
        assertEquals(1740/*!!! BUT QUEUE HAS MORE ELEMENTS THAN WITH BATCH!!!*/, cs.getQueueForTest().calculateSize());
    }

    @Test
    public void test_real_life_no_batch() {
        //real life simulation no batch
        final JskCs<Integer, String> cs = JskCs
                .simple(IntStream.range(0, 100).mapToObj($ -> new JskTestSource($, 20)).toList(), COMP);
        makeRealLifeExperimentAndCheck(cs);
        assertEquals(108/*!!! TOO MUCH WITH SIMPLE STRATEGY, NEED SMTH MORE COMPLEX LIKE BATCH !!!*/, cs.getExpandsDone());
        assertEquals(1066/*!!! BUT QUEUE HAS FEWER ELEMENTS COMPARED TO BATCH!!!*/, cs.getQueueForTest().calculateSize());
    }

    @Test
    public void test_gets_with_non_single_elements() {
        //many get
        final JskCs<Integer, String> simpleClusterSorter = JskCs.simple(Cc.l(
                new JskTestSource(0, 2),
                new JskTestSource(1, 3),
                new JskTestSource(2, 4),
                new JskTestSource(3, 5)
        ), COMP);

        assertEquals("", format(simpleClusterSorter.getNext(0)));
        assertEquals("0-0💼,0-1💼,0-2💼,0-3💼", format(simpleClusterSorter.getQueueForTest()));

        assertEquals("0-0,0-1", format(simpleClusterSorter.getNext(2)));
        assertEquals("0-2💼,0-3💼,1-0,1-1💼", format(simpleClusterSorter.getQueueForTest()));

        assertEquals("0-2,0-3,1-0", format(simpleClusterSorter.getNext(3)));
        assertEquals("1-1💼,1-2,1-3,2-2,2-3💼,3-2", format(simpleClusterSorter.getQueueForTest()));

        assertEquals("1-1,1-2,1-3,2-1", format(simpleClusterSorter.getNext(4)));
        assertEquals("2-2,2-3💼,3-2", format(simpleClusterSorter.getQueueForTest()));

        assertEquals("2-2,2-3,3-2", format(simpleClusterSorter.getNext(3)));
        assertEquals("3-3,4-3", format(simpleClusterSorter.getQueueForTest()));

        assertEquals("3-3,4-3", format(simpleClusterSorter.getNext(5)));
        assertEquals("", format(simpleClusterSorter.getQueueForTest()));

        assertEquals(6, simpleClusterSorter.getExpandsDone());
    }

    @Test
    public void test_one_element_gets() {
        //single get
        final List<JskTestSource> sources = Cc.l(
                new JskTestSource(0, 2),
                new JskTestSource(1, 3),
                new JskTestSource(2, 4),
                new JskTestSource(3, 5)
        );
        final JskCs<Integer, String> simpleClusterSorter = JskCs.simple(sources, COMP);

        assertEquals("", format(simpleClusterSorter.getNext(0)));
        assertEquals("0-0💼,0-1💼,0-2💼,0-3💼", format(simpleClusterSorter.getQueueForTest()));

        List<String> expectedSequence = Cc.sort(sources.stream()
                .flatMap(src -> IntStream.range(0, src.getMaxElements()).mapToObj(el -> "%d-%d".formatted(el, src.getId())))
                .collect(Collectors.toList()), COMP);

        int counter = 0;
        while (simpleClusterSorter.hasNext(0)) {
            assertEquals(expectedSequence.get(counter), format(simpleClusterSorter.getNext(1)));
            counter++;
        }
    }

    private void makeRealLifeExperimentAndCheck(JskCs<Integer, String> cs) {
        List<String> expectedSequence =
                Cc.sort(IntStream.range(0, 100).mapToObj(i -> i)
                        .flatMap(i -> IntStream.range(0, 20).mapToObj(j -> "%d-%d".formatted(j, i)))
                        .collect(Cc.toL()), COMP);


        final int increment = 25;
        for (int i = 0; i < 250; i += increment) {
            final List<String> items = cs.getNext(increment);
            for (int j = 0; j < items.size(); j++) {
                assertEquals(expectedSequence.get(i + j), items.get(j));
            }
        }
        assertEquals("2-50,2-51,2-52,2-53,2-54,2-55,2-56,2-57,2-58,2-59", format(cs.getNext(10)));
    }

    private static String format(List<String> lst) {
        return Cc.join(lst);
    }

    private static String format(JskCsQueue<Integer, String> queue) {
        return Cc.stream(queue).map($ -> $.getItem() + ($.isExpandableLastItem() ? E : "")).collect(Collectors.joining(","));
    }

    @RequiredArgsConstructor
    @Getter
    private static class JskTestBatch implements JscCsBatchProcessor<Integer, String> {
        @Override
        public Map<Integer, JskCsList<String>> getNextElements(List<JskCsSource<Integer, String>> sourcesToBatch,
                Map<Integer, Integer> neededCountsPerSource) {
            return sourcesToBatch.stream()
                    .map($ -> X.x($.getId(), $.getNextElements(neededCountsPerSource.get($.getId()))))
                    .collect(Cc.toMX2());
        }
    }

    @RequiredArgsConstructor
    @Getter
    private static class JskTestSource implements JskCsSource<Integer, String> {
        final Integer id;
        final int maxElements;

        List<String> producedElements = Cc.l();

        @Override
        public String toString() {
            return id + "";
        }

        @Override
        public JskCsList<String> getNextElements(int limit) {
            final int couldProduceMore = maxElements - producedElements.size();
            int willProduceNow = Math.min(couldProduceMore, limit);
            final List<String> items =
                    IntStream.range(0, willProduceNow).mapToObj(i -> "%d-%d".formatted(producedElements.size() + i, id))
                            .toList();
            producedElements.addAll(items);
            return new JskCsList<>(items, producedElements.size() < maxElements);
        }

    }

    private static class Comparator implements java.util.Comparator<String> {
        int[] parse(String p) {
            return Arrays.stream(p.split("-")).mapToInt($ -> Ma.pi($)).toArray();
        }

        @Override
        public int compare(String o1, String o2) {
            final int[] o1V = parse(o1);
            final int[] o2V = parse(o2);
            final int c1 = Integer.compare(o1V[0], o2V[0]);
            final int c2 = Integer.compare(o1V[1], o2V[1]);
            return c1 != 0 ? c1 : c2;
        }
    }

}
