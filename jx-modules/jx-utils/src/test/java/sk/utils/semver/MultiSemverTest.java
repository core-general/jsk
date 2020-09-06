package sk.utils.semver;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import static org.junit.Assert.*;

public class MultiSemverTest {

    @Test
    public void isGreaterThan() {
        assertEquals(v("1.0.0"), v("1.0"));
        assertTrue(v("1.0.1").isGreaterThan(v("1.0.0")));
        assertTrue(v("1.0.1.1").isGreaterThan(v("1.0.1")));
        assertFalse(v("1.0.1.1").isGreaterThan(v("1.0.1.2")));
        assertFalse(v("1.0.1.1").isGreaterThan(v("2.0.0.0")));
        assertEquals(v("1"), v("1"));
        assertFalse(v("1").isGreaterThan(v("1")));
    }


    @Test
    public void isGreaterOrEqualThan() {
        assertEquals(v("1.0.0"), v("1.0"));
        assertTrue(v("1.0.1").isGreaterOrEqualThan(v("1.0.0")));
        assertTrue(v("1.0.1.1").isGreaterOrEqualThan(v("1.0.1")));
        assertFalse(v("1.0.1.1").isGreaterOrEqualThan(v("1.0.1.2")));
        assertFalse(v("1.0.1.1").isGreaterOrEqualThan(v("2.0.0.0")));
        assertEquals(v("1"), v("1"));
        assertTrue(v("1").isGreaterOrEqualThan(v("1")));
    }


    private MultiSemver v(String s) {
        return MultiSemver.parse(s).orElse(null);
    }
}
