package sk.utils.statics;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaTest {

    @Test
    public void isInt() {
        assertTrue(Ma.isInt("1234567890"));
        assertTrue(Ma.isInt("1"));
        assertTrue(Ma.isInt("9"));

        assertFalse(Ma.isInt("1.1"));
        assertFalse(Ma.isInt("1234567890a"));
        assertFalse(Ma.isInt("a1234567890"));
        assertFalse(Ma.isInt("a"));
        assertFalse(Ma.isInt(""));
    }

    @Test
    public void isFloat() {
        assertTrue(Ma.isFloat("1"));
        assertTrue(Ma.isFloat("1"));
        assertTrue(Ma.isFloat("9"));
        assertTrue(Ma.isFloat("1.1"));
        assertTrue(Ma.isFloat("22221.12222"));

        assertFalse(Ma.isFloat("1."));
        assertFalse(Ma.isFloat(".1"));
        assertFalse(Ma.isFloat("1234567890a"));
        assertFalse(Ma.isFloat("1234567.890a"));
        assertFalse(Ma.isFloat("a1234567890"));
        assertFalse(Ma.isFloat("a123456,7890"));
        assertFalse(Ma.isFloat("a"));
        assertFalse(Ma.isFloat(""));
    }
}
