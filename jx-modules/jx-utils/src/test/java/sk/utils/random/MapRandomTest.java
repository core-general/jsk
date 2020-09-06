package sk.utils.random;

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
import sk.test.MockitoTest;
import sk.utils.paging.RingPicker;
import sk.utils.statics.Cc;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static sk.utils.random.MapRandom.rnd;

public class MapRandomTest extends MockitoTest {
    @Test
    public void general() {
        Iterator<Double> cp = RingPicker.create(Cc.l(0.55d, 0.44d, 0.19d)).iterator();
        assertEquals(rnd(cp::next, Cc.m("a", 2d, "b", 3d, "c", 5d)), "c");
        assertEquals(rnd(cp::next, Cc.m("a", 2d, "b", 3d, "c", 5d)), "b");
        assertEquals(rnd(cp::next, Cc.m("a", 2d, "b", 3d, "c", 5d)), "a");
    }
}
