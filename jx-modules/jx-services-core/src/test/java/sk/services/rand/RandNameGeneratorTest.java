package sk.services.rand;

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

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class RandNameGeneratorTest {
    @Test
    public void randNames() {
        final Random random = new Random(0);
        final IRand iRand = () -> random;
        assertEquals(new RandNameGenerator(iRand, 1).generateNext(), "Shyth");
        assertEquals(new RandNameGenerator(iRand, 2).generateNext(), "Glouz Toss");
        assertEquals(new RandNameGenerator(iRand, 3).generateNext(), "Flubeist Oiditt Daivoir");
    }
}
