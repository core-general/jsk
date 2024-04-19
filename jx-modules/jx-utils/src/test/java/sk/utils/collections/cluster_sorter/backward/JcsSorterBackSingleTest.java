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
import sk.utils.collections.cluster_sorter.backward.impl.JcsSorterBack;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JcsSorterBackSingleTest extends JcsSorterBackGeneralTest {
    @Test
    public void test_normal_scenario_start_from_begining() {
        var sorter = createSorter(sources, COMP);

        assertEquals("", format(sorter.getNext(0)));
        assertEquals(" <-B|F-> 0-0💼-> 0-1💼-> 0-2💼-> 0-3💼->", formatFull(sorter.getQueue()));

        assertEquals("0-0 0-1 0-2", format(sorter.getNext(3)));
        assertEquals("0-0 0-1 0-2 <-B|F-> 0-3💼-> 1-0 1-1 1-2💼-> 2-1", formatFull(sorter.getQueue()));

        assertEquals("0-3 1-0", format(sorter.getNext(2)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 <-B|F-> 1-1 1-2💼-> 1-3 2-1 2-3💼->", formatFull(sorter.getQueue()));

        assertEquals("1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3", format(sorter.getNext(12)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3 <-B|F-> ", formatFull(sorter.getQueue()));

        assertEquals("", format(sorter.getNext(12)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3 <-B|F-> ", formatFull(sorter.getQueue()));

        assertEquals("4-3 3-3 3-2", format(sorter.getPrevious(3)));
        assertEquals("0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 <-B|F-> 3-2 3-3 4-3", formatFull(sorter.getQueue()));

        assertEquals("2-3 2-2 2-1 1-3 1-2 1-1 1-0 0-3 0-2 0-1 0-0", format(sorter.getPrevious(100)));
        assertEquals(" <-B|F-> 0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3", formatFull(sorter.getQueue()));

        assertEquals("", format(sorter.getPrevious(100)));
        assertEquals(" <-B|F-> 0-0 0-1 0-2 0-3 1-0 1-1 1-2 1-3 2-1 2-2 2-3 3-2 3-3 4-3", formatFull(sorter.getQueue()));
    }

    @Test
    public void test_normal_scenario_start_from_end() {
        var backSorter = createSorter(sources, COMP);

        backSorter.setPositionToItem("10-10");

        assertEquals("", format(backSorter.getNext(0)));
        assertEquals("0-0 1-0 1-1<-💼 2-1 2-2<-💼 3-2 3-3<-💼 4-3 <-B|F-> ", formatFull(backSorter.getQueue()));

        assertEquals("4-3 3-3 3-2", format(backSorter.getPrevious(3)));
        assertEquals("0-0 1-0 1-1<-💼 1-3<-💼 2-1 2-2<-💼 2-3 <-B|F-> 3-2 3-3 4-3", formatFull(backSorter.getQueue()));

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
        return JcsSorterBack.simple(sources, comp);
    }
}
