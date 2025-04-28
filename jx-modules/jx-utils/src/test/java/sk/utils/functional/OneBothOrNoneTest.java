package sk.utils.functional;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static sk.utils.functional.OneBothOrNone.any;

class OneBothOrNoneTest {
    @Test
    void genericTest() {
        assertEquals("d x", oneOrBoth("d", "x"));
        assertEquals("z", oneOrBoth("z", null));
        assertEquals("c", oneOrBoth(null, "c"));
        assertNull(oneOrBoth(null, null));
    }

    @Test
    void badMapTest() {
        assertNull(any("a", null)
                .map((o) -> null, (o) -> null)
                .collect(OneOf::collectSelf, b -> b, () -> null));
        assertEquals("123", any((O<?>) null, (O<?>) null)
                .map((o) -> "123", (o) -> null)
                .collect(oneOf -> oneOf.left(), b -> b, () -> null));
    }

    private static String oneOrBoth(String a, String b) {
        return any(a, b)
                .collect(OneOf::collectSelf, both -> both.collect((l, r) -> "%s %s".formatted(l, r)), () -> null);
    }
}
