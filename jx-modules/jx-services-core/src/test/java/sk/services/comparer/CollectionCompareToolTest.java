package sk.services.comparer;

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
import sk.services.comparer.model.SetCompareResult;
import sk.utils.statics.Cc;

import static org.junit.Assert.*;

public class CollectionCompareToolTest {

    @Test
    public void compare() {
        SetCompareResult<String> res = SetCompareTool.compare(Cc.s("a", "b", "c"), Cc.s("e", "d", "c"));
        assertTrue(res.hasDifferences());
        assertTrue(res.getNotExistingIn2().containsAll(Cc.l("a", "b")));
        assertEquals(res.getNotExistingIn2().size(), 2);
        assertTrue(res.getNotExistingIn1().containsAll(Cc.l("e", "d")));
        assertEquals(res.getNotExistingIn1().size(), 2);

        res = SetCompareTool.compare(Cc.s("a", "b", "c"), Cc.s("c"));
        assertTrue(res.hasDifferences());
        assertTrue(res.getNotExistingIn2().containsAll(Cc.l("a", "b")));
        assertEquals(res.getNotExistingIn2().size(), 2);
        assertTrue(res.getNotExistingIn1().containsAll(Cc.l()));
        assertEquals(res.getNotExistingIn1().size(), 0);

        res = SetCompareTool.compare(Cc.s("c"), Cc.s("e", "d", "c"));
        assertTrue(res.hasDifferences());
        assertTrue(res.getNotExistingIn2().containsAll(Cc.l()));
        assertEquals(res.getNotExistingIn2().size(), 0);
        assertTrue(res.getNotExistingIn1().containsAll(Cc.l("e", "d")));
        assertEquals(res.getNotExistingIn1().size(), 2);

        res = SetCompareTool.compare(Cc.s("a"), Cc.s("a"));
        assertFalse(res.hasDifferences());
        assertTrue(res.getNotExistingIn2().containsAll(Cc.l()));
        assertEquals(res.getNotExistingIn2().size(), 0);
        assertTrue(res.getNotExistingIn1().containsAll(Cc.l()));
        assertEquals(res.getNotExistingIn1().size(), 0);
    }
}
