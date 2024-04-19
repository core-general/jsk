package sk.utils.collections.cluster_sorter.forward.impl;

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
import org.junit.jupiter.api.Test;
import sk.utils.collections.cluster_sorter.JcsAbstractCsTest;
import sk.utils.collections.cluster_sorter.abstr.JcsASorter;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JcsIForwardBatch;
import sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JcsSorterForwardTest extends JcsAbstractCsTest<JcsAbstractCsTest.JcsTestSource> {
    @Override
    protected JcsTestSource createTestSource(int id, int count) {
        return new JcsTestSource(id, count);
    }

    @Override
    protected JcsASorter<?, ?, ?, ?> createSorter(List<JcsTestSource> sources, JcsComparator comp) {
        return JcsSorterForward.simple(sources, comp);
    }

    @Test
    public void test_real_life_with_batch() {
        //real life simulation with batch
        final JcsSorterForward<String, JcsTestSource> cs = JcsSorterForward
                .batch(IntStream.range(0, 100).mapToObj($ -> new JcsTestSource($, 20)).toList(), COMP, new JcsTestBatch());
        makeRealLifeExperimentAndCheck(cs);
        assertEquals(4/*!!!MUCH LESS WITH BATCH !!!*/, cs.getExpandsDone());
        assertEquals(1740/*!!! BUT QUEUE HAS MORE ELEMENTS THAN WITHOUT BATCH!!!*/, cs.getQueue()
                .calculateSize(Cc.s(JcsEForwardType.FORWARD)));
    }

    @Test
    public void test_real_life_no_batch() {
        //real life simulation no batch
        final JcsSorterForward<String, JcsTestSource> cs = JcsSorterForward
                .simple(IntStream.range(0, 100).mapToObj($ -> new JcsTestSource($, 20)).toList(), COMP);
        makeRealLifeExperimentAndCheck(cs);
        assertEquals(108/*!!! TOO MUCH WITH SIMPLE STRATEGY, NEED SMTH MORE COMPLEX LIKE BATCH !!!*/, cs.getExpandsDone());
        assertEquals(1066/*!!! BUT QUEUE HAS FEWER ELEMENTS COMPARED TO BATCH!!!*/,
                cs.getQueue().calculateSize(Cc.s(JcsEForwardType.FORWARD)));
    }

    @Test
    public void test_remove_source() {
        var sorter = JcsSorterForward.simple(
                IntStream.range(0, 5).mapToObj($ -> new JcsTestSource($, 20)).toList(), COMP);

        assertEquals("0-0 0-1 0-2 0-3 0-4 1-0 1-1 1-2 1-3 1-4", format(sorter.getNext(10)));
        assertEquals("2-0💼 2-1💼 2-2💼 2-3💼 2-4💼", format(sorter.getQueue()));
        assertEquals("2-0", format(sorter.removeSource(new JcsSourceId("0"))));
        assertEquals("2-2", format(sorter.removeSource(new JcsSourceId("2"))));

        assertEquals("2-1💼 2-3💼 2-4💼", format(sorter.getQueue()));
        assertEquals("2-1 2-3 2-4 3-1 3-3", format(sorter.getNext(5)));
        assertEquals("3-4 4-1 4-3 4-4 5-1 5-3 5-4💼 6-1 6-3💼 7-1💼", format(sorter.getQueue()));
    }

    @Test
    public void test_add_source() {
        var sorter = JcsSorterForward.batch(
                IntStream.range(0, 20)
                        .filter($ -> $ % 2 == 1)//leave only odd
                        .mapToObj($ -> new JcsTestSource($, 1000)).toList(), COMP,
                new JcsTestBatch());
        sorter.addNewSource(new JcsTestSource(2, 1));
        assertEquals("0-1 0-2 0-3 0-5 0-7 0-9", format(sorter.getNext(6)));
        sorter.addNewSource(new JcsTestSource(12, 1));
        sorter.addNewSource(new JcsTestSource(14, 1));
        assertEquals("0-11 0-12 0-13 0-14 0-15", format(sorter.getNext(5)));

        sorter.getNext(1001);
        assertEquals("100-19 101-1 101-3 101-5 101-7", format(sorter.getNext(5)));

        assertThrows(RuntimeException.class, () -> sorter.addNewSource(new JcsTestSource(2, 2)));

        sorter.addNewSource(new JcsTestSource(8, 100));//will not get into queue
        assertEquals("101-9 101-11 101-13 101-15 101-17", format(sorter.getNext(5)));
        sorter.addNewSource(new JcsTestSource(18, 1000));//will get into queue
        sorter.addNewSource(new JcsTestSource(20, 1000));
        assertEquals("101-18 101-19 101-20 102-1 102-3", format(sorter.getNext(5)));
    }

    @Test
    public void test_gets_with_non_single_elements() {
        //many get
        final JcsSorterForward<String, JcsTestSource> simpleClusterSorter = JcsSorterForward.simple(Cc.l(
                new JcsTestSource(0, 2),
                new JcsTestSource(1, 3),
                new JcsTestSource(2, 4),
                new JcsTestSource(3, 5)
        ), COMP);

        assertEquals("", format(simpleClusterSorter.getNext(0)));
        assertEquals("0-0💼 0-1💼 0-2💼 0-3💼", format(simpleClusterSorter.getQueue()));

        assertEquals("0-0 0-1", format(simpleClusterSorter.getNext(2)));
        assertEquals("0-2💼 0-3💼 1-0 1-1💼", format(simpleClusterSorter.getQueue()));

        assertEquals("0-2 0-3 1-0", format(simpleClusterSorter.getNext(3)));
        assertEquals("1-1💼 1-2 1-3 2-2 2-3💼 3-2", format(simpleClusterSorter.getQueue()));

        assertEquals("1-1 1-2 1-3 2-1", format(simpleClusterSorter.getNext(4)));
        assertEquals("2-2 2-3💼 3-2", format(simpleClusterSorter.getQueue()));

        assertEquals("2-2 2-3 3-2", format(simpleClusterSorter.getNext(3)));
        assertEquals("3-3 4-3", format(simpleClusterSorter.getQueue()));

        assertEquals("3-3 4-3", format(simpleClusterSorter.getNext(5)));
        assertEquals("", format(simpleClusterSorter.getQueue()));

        assertEquals(6, simpleClusterSorter.getExpandsDone());
    }

    @Test
    public void test_one_element_gets() {
        //single get
        final List<JcsTestSource> sources = Cc.l(
                new JcsTestSource(0, 2),
                new JcsTestSource(1, 3),
                new JcsTestSource(2, 4),
                new JcsTestSource(3, 5)
        );
        final JcsSorterForward<String, JcsTestSource> simpleClusterSorter = JcsSorterForward.simple(sources, COMP);

        assertEquals("", format(simpleClusterSorter.getNext(0)));
        assertEquals("0-0💼 0-1💼 0-2💼 0-3💼", format(simpleClusterSorter.getQueue()));

        List<String> expectedSequence = Cc.sort(sources.stream()
                .flatMap(src -> IntStream.range(0, src.getMaxElements()).mapToObj(el -> "%d-%s".formatted(el, src.getSourceId())))
                .collect(Collectors.toList()), COMP);

        int counter = 0;
        while (simpleClusterSorter.hasNext(0)) {
            assertEquals(expectedSequence.get(counter), format(simpleClusterSorter.getNext(1)));
            counter++;
        }
    }

    private void makeRealLifeExperimentAndCheck(JcsSorterForward<String, JcsTestSource> cs) {
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
        assertEquals("2-50 2-51 2-52 2-53 2-54 2-55 2-56 2-57 2-58 2-59", format(cs.getNext(10)));
    }


    protected String format(JcsQueueForward<String, JcsTestSource> queue) {
        return Cc.list(queue.getDirectionIterators().get(JcsEForwardType.FORWARD)).stream()
                .map($ -> $.getItem() + ($.isExpandable() ? E : "")).collect(Collectors.joining(" "));
    }

    @RequiredArgsConstructor
    @Getter
    private static class JcsTestBatch implements JcsIForwardBatch<String, JcsTestSource> {
        @Override
        public Map<JcsSourceId, Map<JcsEForwardType, JcsList<String>>> getNextElements(
                Collection<JcsTestSource> sourcesToBatch,
                Map<JcsSourceId, Map<JcsEForwardType, Integer>> neededCountsPerSourcePerDirection) {
            return sourcesToBatch.stream()
                    .map($ -> X.x($.getSourceId(),
                            Cc.m(JcsEForwardType.FORWARD, $.getNextUnseenElements(
                                    neededCountsPerSourcePerDirection.get($.getSourceId()).get(JcsEForwardType.FORWARD)))))
                    .collect(Cc.toMX2());
        }
    }
}
