package sk.utils.tree;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import static org.junit.Assert.*;
import static sk.utils.tree.TreePath.path;

public class TreePathTest extends MockitoTest {

    @Test
    public void testMerge() {
        assertEquals(path("a", "c").merge(path("b", "d")).toString(), "a/c/b/d");
    }

    @Test
    public void testGetParentAndToStringWith() {
        assertEquals(path("a", "b", "c").getParent().toStringWith("-"), "a-b");
    }

    @Test
    public void testGetLeaf() {
        assertEquals(path("a", "b").getLeaf(), "b");
    }

    @Test
    public void testCreate() {
        assertEquals("", path().getPath());
        assertEquals("", path("").getPath());
        assertEquals("a/b/c", path("a", "b", "c").getPath());
        assertEquals("a", path("a").getPath());
    }

    @Test
    public void testIsParentFor() {
        assertTrue(path().isParentOf(path("a", "b", "c")));
        assertTrue(path("a", "b").isParentOf(path("a", "b", "c")));
        assertTrue(path("a", "b").isParentOf(path("a", "b", "c", "d")));
        assertTrue(path("a", "b").isParentOf(path("a", "b")));

        assertFalse(path("a", "b").isParentOf(path("a")));
        assertFalse(path("a", "b").isParentOf(path("a", "c")));
        assertFalse(path("a", "b").isParentOf(path("b", "c")));
        assertFalse(path("a", "b").isParentOf(path("a", "c", "c")));
    }
}
