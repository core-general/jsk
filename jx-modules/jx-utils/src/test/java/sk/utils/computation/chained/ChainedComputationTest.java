package sk.utils.computation.chained;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import lombok.ToString;
import org.junit.jupiter.api.Test;
import sk.utils.computation.chained.values.MappedComputedValue;
import sk.utils.computation.chained.values.SimpleComputedValue;
import sk.utils.computation.chained.values.SomeValue;
import sk.utils.computation.chained.values.StaticValue;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ChainedComputationTest {
    @Test
    public void calculate() {
        calculate(true);
        calculate(false);
    }

    public void calculate(boolean parallel) {
        {
            ChainedComputation cc = new ChainedComputation(
                    Cc.m(
                            "testAB", new StaticValue<>(new B(), B.class),
                            "constant1", new StaticValue<>(100, Integer.class),
                            "constant2", new StaticValue<>("abc", String.class),
                            "constant3", new StaticValue<>(Cc.l(1, 2, 3, 4, 5), TypeWrap.getList(Integer.class)),

                            "simpleCalculation1",
                            new SimpleComputedValue<>(chainedComputation -> chainedComputation.getDataHolder().size(),
                                    Integer.class),

                            "mappedCalculation2",
                            new MappedComputedValue<>(tmi -> tmi.simpleCalculation1 * 3 + "", TestMappedInput.class,
                                    String.class),

                            "mappedCalculation3",
                            new MappedComputedValue<>(tmi -> tmi.constant3.stream().mapToInt($ -> $).sum() + "",
                                    TestMappedInput2.class, String.class)
                    )
                    , parallel);

            final TestMappedInput3 calculate = cc.calculate(TestMappedInput3.class);
            assertEquals(calculate.mappedCalculation3, "15");
            assertEquals(cc.getDataHolder().get("mappedCalculation2").get(cc), "21");
            assertEquals(cc.getDataHolder().get("simpleCalculation1").get(cc), 7);
        }

        {


            final Map<String, SomeValue<?>> data = Cc.m(
                    "constant1", new StaticValue<>(100, Integer.class),
                    "constant2", new StaticValue<>("abc", String.class),
                    "constant3", new StaticValue<>(Cc.l(1, 2, 3, 4, 5), TypeWrap.getList(Integer.class))
            );

            assertThrows(InvocationTargetException.class,
                    () -> new ChainedComputation(data, parallel).calculate(TestMappedInput4.class));
            assertThrows(InvocationTargetException.class,
                    () -> new ChainedComputation(data, parallel).calculate(TestMappedInput5.class));
            assertThrows(InvocationTargetException.class,
                    () -> new ChainedComputation(data, parallel).calculate(TestMappedInput6.class));
            assertThrows(InvocationTargetException.class,
                    () -> new ChainedComputation(data, parallel).calculate(TestMappedInput7.class));
        }

    }

    @ToString
    private static class TestMappedInput extends ChainedMappedInput {
        public A testAB;
        public Integer constant1;
        public String constant2;
        public List<Integer> constant3;

        public Integer simpleCalculation1;

        public TestMappedInput(ChainedComputation context) {
            super(context);
        }
    }

    @ToString(callSuper = true)
    private static class TestMappedInput2 extends TestMappedInput {
        public String mappedCalculation2;

        public TestMappedInput2(ChainedComputation context) {
            super(context);
        }
    }

    private static class TestMappedInput3 extends ChainedMappedInput {
        public String mappedCalculation3;

        public TestMappedInput3(ChainedComputation context) {
            super(context);
        }
    }

    private static class TestMappedInput4 extends ChainedMappedInput {
        public String constant1;

        public TestMappedInput4(ChainedComputation context) {
            super(context);
        }
    }

    private static class TestMappedInput5 extends ChainedMappedInput {
        public Integer constant2;

        public TestMappedInput5(ChainedComputation context) {
            super(context);
        }
    }

    private static class TestMappedInput6 extends ChainedMappedInput {
        public List<Integer> constant2;

        public TestMappedInput6(ChainedComputation context) {
            super(context);
        }
    }

    private static class TestMappedInput7 extends ChainedMappedInput {
        public List<Double> constant3;

        public TestMappedInput7(ChainedComputation context) {
            super(context);
        }
    }

    public interface A {}

    public static class B implements A {}
}
