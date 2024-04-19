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

import org.junit.jupiter.api.Test;
import sk.utils.statics.Cc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RandTestImplTest {
    RandTestImpl rti = new RandTestImpl();

    @Test
    public void rndManyFromListTest() {
        final List<String> l = Cc.l("a", "b", "c", "d");
        for (int i = 0; i < 20; i++) {
            assertEquals(rti.rndManyFromListAndSort(2, l).size(), 2);
            assertEquals(rti.rndManyFromListAndSort(4, l).size(), 4);
            assertEquals(rti.rndManyFromListAndSort(5, l).size(), 4);
            assertEquals(rti.rndManyFromListAndSort(1, l).size(), 1);
            assertEquals(rti.rndManyFromListAndSort(0, l).size(), 0);
        }
    }
}
