package sk.db.relational.utils.cluster_sorter;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

public class DynamicPagingHelperTest {
    @Test
    public void helpTest() {
        assertEquals(new JcsDynamicPagingHelper(2, 2, 0, 1), JcsDynamicPagingHelper.help(4, 2));
        assertEquals(new JcsDynamicPagingHelper(1, 3, 0, 1), JcsDynamicPagingHelper.help(3, 2));
        assertEquals(new JcsDynamicPagingHelper(1, 4, 1, 2), JcsDynamicPagingHelper.help(5, 2));
        assertEquals(new JcsDynamicPagingHelper(1, 10, 2, 8), JcsDynamicPagingHelper.help(12, 7));
        assertEquals(new JcsDynamicPagingHelper(100, 5, 0, 4), JcsDynamicPagingHelper.help(500, 5));
        assertEquals(new JcsDynamicPagingHelper(50, 10, 1, 5), JcsDynamicPagingHelper.help(501, 5));
    }
}
