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

public class MilliNumTest {
    @Test
    public void toNonMilli() {
        assertEquals("15.13", MilliNum.create("15.1343").toNonMilli());
        assertEquals("111.10", MilliNum.create("111.1").toNonMilli());
        assertEquals("111.01", MilliNum.create("111.01").toNonMilli());
        assertEquals("111.00", MilliNum.create("111").toNonMilli());
        assertEquals(11110L, MilliNum.create("111.1").getMultValue());
    }
}
