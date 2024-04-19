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
import org.junit.jupiter.api.Test;
import sk.utils.collections.cluster_sorter.abstr.JcsASorter;
import sk.utils.collections.cluster_sorter.abstr.JcsASource;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.statics.Cc;
import sk.utils.statics.Ma;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class JcsAbstractCsTest<T extends JcsAbstractCsTest.JcsTestSource> {
    protected final String E/*XPANDABLE*/ = "💼";
    protected final JcsComparator COMP = new JcsComparator();

    protected abstract T createTestSource(int id, int count);

    protected abstract JcsASorter<?, ?, ?, ?> createSorter(List<T> sources, JcsComparator comp);

    @Test
    public void test_sameids_throw_exception() {
        assertThrows(RuntimeException.class, () -> createSorter(Cc.l(
                createTestSource(0, 2),
                createTestSource(0, 5)
        ), COMP));
    }

    protected String format(List<String> lst) {
        return Cc.join(" ", lst);
    }

    @Getter
    public static class JcsTestSource extends JcsASource<String> {
        final int maxElements;

        int iteratorForward;

        public JcsTestSource(int id, int maxElements) {
            this(id, maxElements, 0);
        }

        public JcsTestSource(int id, int maxElements, int setElement) {
            super(new JcsSourceId(id + ""));
            this.maxElements = maxElements;
            iteratorForward = setElement;
        }

        @Override
        public String toString() {
            return getSourceId() + "";
        }

        @Override
        public JcsList<String> getNextUnseenElements(int limit) {
            final int couldProduceMore = maxElements - size();
            int willProduceNow = Math.min(couldProduceMore, limit);
            final List<String> items =
                    IntStream.range(0, willProduceNow)
                            .mapToObj(i -> X.x(iteratorForward + i, getSourceId()))
                            .filter($ -> $.i1() < maxElements)
                            .map($ -> formatter($))
                            .toList();
            iteratorForward += items.size();
            return new JcsList<>(items, iteratorForward < maxElements);
        }

        protected String formatter(X2<Integer, JcsSourceId> $) {
            return "%d-%s".formatted($.i1(), $.i2());
        }

        protected int size() {
            return iteratorForward;
        }
    }

    @Getter
    public static class JcsTestBackSource extends JcsTestSource implements JcsIBackSource<String> {
        int iteratorBackward;

        public JcsTestBackSource(int id, int maxElements) {
            this(id, maxElements, 0);
        }

        public JcsTestBackSource(int id, int maxElements, int setElement) {
            super(id, maxElements, setElement);
            iteratorBackward = setElement - 1;
        }

        @Override
        public JcsList<String> getPreviousUnseenElements(int limit) {
            final int couldProduceMore = maxElements - size();
            int willProduceNow = Math.min(couldProduceMore, limit);
            final List<String> items =
                    IntStream.range(0, willProduceNow)
                            .mapToObj(i -> X.x(iteratorBackward - i, getSourceId()))
                            .filter($ -> $.i1() >= 0)
                            .map($ -> formatter($))
                            .toList();
            iteratorBackward -= items.size();
            return new JcsList<>(items, iteratorBackward >= 0);
        }

        protected int size() {
            return Math.abs(iteratorForward - iteratorBackward) - 1;
        }

        @Override
        public boolean canSetPosition() {
            return true;
        }

        @Override
        public void setPositionToItem(String s) {
            int itemIndex = Ma.pi(s.split("-")[0]);
            iteratorForward = Math.min(itemIndex, maxElements - 1);
            iteratorBackward = iteratorForward - 1;
        }
    }

    protected static class JcsComparator implements java.util.Comparator<String> {
        int[] parse(String p) {
            return new int[]{Ma.pi(St.subRL(p, "-")), Ma.pi(St.subLL(p, "-"))};
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
