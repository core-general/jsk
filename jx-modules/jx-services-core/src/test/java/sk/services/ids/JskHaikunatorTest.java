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
import sk.services.time.ITime;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.statics.Ti;

import java.time.ZoneId;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static sk.services.ids.JskHaikunator.LongAndShortHaikunator;
import static sk.services.ids.JskHaikunator.defaultHaikunators;

public class JskHaikunatorTest {
    @Test
    public void haikunate() {
        final Random rnd = new Random(0);
        final ITime times = new ITime() {
            @Override
            public ZoneId getZone() {
                return Ti.UTC;
            }

            @Override
            public long now() {
                return 10101010L;
            }
        };
        LongAndShortHaikunator haiku = defaultHaikunators(() -> rnd, times);

        assertEquals(haiku.lng().haikunate(), "horror-lower-other-HBFHuR");
        assertEquals(haiku.shrt().haikunate(), "proper-share-iI1");
        assertEquals(haiku.tiny().haikunate(), "united-major");

        assertEquals("plane-inside-civil-322-throat", new JskHaikunator(() -> rnd, times, "A-A-A-R-N", 3, St.dig).haikunate());
        assertEquals("fine-amount-10101010", new JskHaikunator(() -> rnd, times, "A-N-T", 0, St.dig).haikunate());
        assertEquals("just-lift-10101010,floral-tale-10101010,glad-special-10101010",
                Cc.join(new JskHaikunator(() -> rnd, times, "A-N-T", 0, St.dig).haikunate(3)));
    }
}
