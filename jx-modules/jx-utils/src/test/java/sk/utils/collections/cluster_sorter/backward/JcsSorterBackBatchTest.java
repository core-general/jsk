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

import org.junit.jupiter.api.Test;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.impl.JcsSorterBack;
import sk.utils.statics.Cc;
import sk.utils.tuples.X2;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JcsSorterBackBatchTest extends JcsSorterBackGeneralTest {
    @Test
    public void test_problem_with_sequential_elements_in_source() {
        var backSorter = createSorter(Cc.l(
                new JcsTestBackSource(0, 5) {
                    @Override
                    protected String formatter(X2<Integer, JcsSourceId> $) {
                        return "%s-%d".formatted($.i2(), $.i1());
                    }
                },
                new JcsTestBackSource(1, 5) {
                    @Override
                    protected String formatter(X2<Integer, JcsSourceId> $) {
                        return "%s-%d".formatted($.i2(), $.i1());
                    }
                },
                new JcsTestBackSource(2, 5) {
                    @Override
                    protected String formatter(X2<Integer, JcsSourceId> $) {
                        return "%s-%d".formatted($.i2(), $.i1());
                    }
                }
        ), COMP);

        backSorter.setPositionToItem(
                "4-0");//dirty hack to make cases in back queue, because formatter is overridden in sources above
        assertEquals("", format(backSorter.getPreviousSortedForward(0)));
        assertEquals("0-3<-💼 0-4 1-3<-💼 1-4 2-3<-💼 2-4 <-B|F-> ", formatFull(backSorter.getQueue()));
        assertEquals("1-0 1-1 1-2 1-3 1-4 2-0 2-1 2-2 2-3 2-4", format(backSorter.getPreviousSortedForward(10)));
        assertEquals("0-1<-💼 0-2 0-3 0-4 <-B|F-> 1-0 1-1 1-2 1-3 1-4 2-0 2-1 2-2 2-3 2-4", formatFull(backSorter.getQueue()));
    }

    @Test
    public void test_normal_scenario_start_from_end() {
        var backSorter = createSorter(sources, COMP);

        backSorter.setPositionToItem("10-10");

        assertEquals("", format(backSorter.getNext(0)));
        assertEquals("0-0 1-0 1-1<-💼 2-1 2-2<-💼 3-2 3-3<-💼 4-3 <-B|F-> ", formatFull(backSorter.getQueue()));

        assertEquals("4-3 3-3 3-2", format(backSorter.getPrevious(3)));
        assertEquals("0-0 1-0 1-1<-💼 1-2<-💼 2-1 2-2 2-3<-💼 <-B|F-> 3-2 3-3 4-3", formatFull(backSorter.getQueue()));

        assertEquals("2-3 2-2 2-1 1-3 1-2 1-1 1-0 0-3 0-2 0-1 0-0", format(backSorter.getPrevious(15)));
        assertEquals(" <-B|F-> 0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3", formatFull(backSorter.getQueue()));

        assertEquals("", format(backSorter.getPrevious(15)));
        assertEquals(" <-B|F-> 0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3", formatFull(backSorter.getQueue()));

        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2", format(backSorter.getNext(7)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 <-B|F-> 1-3 2-1 2-2 2-3 3-2 3-3 4-3", formatFull(backSorter.getQueue()));

        assertEquals("1-3 2-1 2-2 2-3 3-2 3-3 4-3", format(backSorter.getNext(7)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3 <-B|F-> ", formatFull(backSorter.getQueue()));

        assertEquals("", format(backSorter.getNext(7)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3 <-B|F-> ", formatFull(backSorter.getQueue()));
    }

    @Override
    protected JcsSorterBack<String, JcsTestBackSource> createSorter(List<JcsTestBackSource> sources, JcsComparator comp) {
        return JcsSorterBack.batch(sources, comp, new JcsTestBatchBack());
    }
}
