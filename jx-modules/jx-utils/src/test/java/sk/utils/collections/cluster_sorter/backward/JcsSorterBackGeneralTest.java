package sk.utils.collections.cluster_sorter.backward;

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
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.impl.JcsSorterBack;
import sk.utils.collections.cluster_sorter.backward.impl.strategies.JcsIBackBatch;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sk.utils.collections.cluster_sorter.JcsAbstractCsTest.JcsTestBackSource;
import static sk.utils.collections.cluster_sorter.backward.model.JcsEBackType.BACKWARD;
import static sk.utils.collections.cluster_sorter.backward.model.JcsEBackType.FORWARD;

public abstract class JcsSorterBackGeneralTest extends JcsAbstractCsTest<JcsTestBackSource> {

    protected abstract JcsSorterBack<String, JcsTestBackSource> createSorter(List<JcsTestBackSource> sources, JcsComparator comp);

    final List<JcsTestBackSource> sources = Cc.l(
            createTestSource(0, 2),
            createTestSource(1, 3),
            createTestSource(2, 4),
            createTestSource(3, 5)
    );

    @Test
    public void test_normal_scenario_start_from_middle_1() {
        var backSorter = createSorter(sources, COMP);

        backSorter.setPositionToItem("1-2");

        assertEquals("", format(backSorter.getNext(0)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 <-B|F-> 1-2💼-> 1-3💼-> 2-1", formatFull(backSorter.getQueue()));

        assertEquals("1-1 1-0 0-3 0-2 0-1 0-0", format(backSorter.getPrevious(15)));
        assertEquals(" <-B|F-> 0-0 0-1 0-2 0-3 1-0 1-1 1-2💼-> 1-3💼-> 2-1", formatFull(backSorter.getQueue()));

        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3", format(backSorter.getNext(15)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3 <-B|F-> ", formatFull(backSorter.getQueue()));
    }

    @Test
    public void test_normal_scenario_start_from_middle_2() {
        var backSorter = createSorter(sources, COMP);

        backSorter.setPositionToItem("3-2");

        assertEquals("", format(backSorter.getNext(0)));
        assertEquals("0-0 1-0 1-1<-💼 2-1 2-2<-💼 2-3<-💼 <-B|F-> 3-2 3-3💼->", formatFull(backSorter.getQueue()));

        assertEquals("2-3 2-2 2-1 1-3 1-2 1-1 1-0 0-3 0-2 0-1 0-0", format(backSorter.getPrevious(15)));
        assertEquals(" <-B|F-> 0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3💼->", formatFull(backSorter.getQueue()));

        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3", format(backSorter.getNext(15)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3 <-B|F-> ", formatFull(backSorter.getQueue()));
    }


    @Override
    protected JcsTestBackSource createTestSource(int id, int count) {
        return new JcsTestBackSource(id, count);
    }


    protected String formatFull(JcsIQueueBack<String, JcsTestBackSource> queue) {
        return formatBack(queue, true) + " <-B|F-> " + formatForward(queue);
    }

    protected String formatForward(JcsIQueueBack<String, JcsTestBackSource> queue) {
        return formatWithType(queue, FORWARD, false);
    }

    protected String formatBack(JcsIQueueBack<String, JcsTestBackSource> queue) {
        return formatBack(queue, false);
    }

    protected String formatBack(JcsIQueueBack<String, JcsTestBackSource> queue, boolean reverseBack) {
        return formatWithType(queue, BACKWARD, reverseBack);
    }


    private String formatWithType(JcsIQueueBack<String, JcsTestBackSource> queue, JcsEBackType format, boolean reverseBack) {
        List<JcsItem<String, JcsEBackType, JcsTestBackSource>> items = Cc.list(queue.getDirectionIterators().get(format));
        if (format == BACKWARD && reverseBack) {
            Cc.reverse(items);
        }
        return items.stream()
                .map($ -> $.getItem() + ($.isExpandable() ? ($.getExpandDirection() == FORWARD ? E + "->" : "<-" + E) : ""))
                .collect(Collectors.joining(" "));
    }

    @RequiredArgsConstructor
    @Getter
    public static class JcsTestBatchBack implements JcsIBackBatch<String, JcsTestBackSource> {
        @Override
        public Map<JcsSourceId, Map<JcsEBackType, JcsList<String>>> getNextElements(
                Collection<JcsTestBackSource> sourcesToBatch,
                Map<JcsSourceId, Map<JcsEBackType, Integer>> neededCountsPerSourcePerDirection) {
            return sourcesToBatch.stream()
                    .map($ -> {
                        Map<JcsEBackType, JcsList<String>> map = Cc.m();
                        Integer needForward = neededCountsPerSourcePerDirection.get($.getSourceId()).getOrDefault(FORWARD, 0);
                        Integer needBackward = neededCountsPerSourcePerDirection.get($.getSourceId()).getOrDefault(BACKWARD, 0);
                        if (needForward > 0) {
                            map.put(FORWARD, $.getNextUnseenElements(1));
                        }
                        if (needBackward > 0) {
                            map.put(BACKWARD, $.getPreviousUnseenElements(1));
                        }

                        return X.x($.getSourceId(), map);
                    })
                    .collect(Cc.toMX2());
        }
    }
}
