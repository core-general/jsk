package sk.utils.math;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LDoubleTest {
    @Test
    public void toNonMilli() {
        assertEquals("5.00", LDouble.createMilli("15").plus(15).div(2).minus(10).toStringAsDouble());
        assertEquals("15.00", LDouble.createMilli("15").toStringAsDouble());
        assertEquals("15.13", LDouble.createMilli("15.1343").toStringAsDouble());
        assertEquals("111.10", LDouble.createMilli("111.1").toStringAsDouble());
        assertEquals("111.01", LDouble.createMilli("111.01").toStringAsDouble());
        assertEquals("111.00", LDouble.createMilli("111").toStringAsDouble());
        assertEquals(11110L, LDouble.createMilli("111.1").getDecValueRaw());
        assertEquals(11110000L, LDouble.create("111.1", 5).getDecValueRaw());
        assertEquals(335.8d, LDouble.create("111.1", 5).mult(6).plus(10).minus(5).div(2).toDouble(), 0.00001d);
        assertEquals("1.24", LDouble.createRaw(1.236, 2).toString());
    }
}
