package sk.utils.statics;

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

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sk.utils.statics.Ti.isSequence;

public class TiTest {

    @Test
    public void isSequenceTest() {
        final ZonedDateTime base = ZonedDateTime.now();
        assertTrue(isSequence());
        assertTrue(isSequence(base));
        assertTrue(isSequence(base, base));
        assertTrue(isSequence(base, base, base));

        assertTrue(isSequence(base, base.plus(1, MILLIS)));
        assertTrue(isSequence(base, base.plus(1, MILLIS), base.plus(1, MILLIS)));
        assertTrue(isSequence(base, base.plus(1, MILLIS), base.plus(2, MILLIS)));
        assertTrue(isSequence(base, base.plus(1, MILLIS), base.plus(1, MILLIS), base.plus(2, MILLIS)));

        assertFalse(isSequence(base, base.minus(1, MILLIS)));
        assertFalse(isSequence(base, base.plus(1, MILLIS), base.minus(1, MILLIS)));
    }
}
