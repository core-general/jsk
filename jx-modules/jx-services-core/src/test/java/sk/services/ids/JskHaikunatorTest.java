package sk.services.ids;

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

import org.junit.Test;
import sk.utils.statics.St;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static sk.services.ids.JskHaikunator.LongAndShortHaikunator;
import static sk.services.ids.JskHaikunator.defaultHaikunators;

public class JskHaikunatorTest {
    @Test
    public void haikunate() {
        final Random rnd = new Random(0);
        LongAndShortHaikunator haiku = defaultHaikunators(() -> rnd);

        assertEquals(haiku.lng().haikunate(), "fragrant-raspy-unit-HBFHuR");
        assertEquals(haiku.shrt().haikunate(), "damp-shadow-iI1");

        assertEquals("polished-black-falling-720-brook", new JskHaikunator(() -> rnd, "A-A-A-R-N", 3, St.dig).haikunate());
    }
}
