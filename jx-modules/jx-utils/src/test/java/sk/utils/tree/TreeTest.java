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

import org.junit.Before;
import org.junit.Test;
import sk.test.MockitoTest;

import static org.junit.Assert.*;
import static sk.utils.functional.O.empty;
import static sk.utils.tree.TreePath.path;

public class TreeTest extends MockitoTest {
    private Tree<Boolean, String> n;

    @Before
    public void setUp() {
        n = Tree.<Boolean, String>create()
                .setVal(path("a"), true)
                .setVal(path("a", "b"), true)
                .setVal(path("a", "b", "c"), false)
                .setVal(path("a", "b", "c"), true)
                .setVal(path("a", "c"), true)
                .setVal(path("b", "b"), true)
                .setVal(path("b", "c"), true)
        ;
    }

    @Test
    public void testToString() {
        assertEquals(n.toString(),
                "ROOT\n" +
                        "|- a\n" +
                        "   |- b\n" +
                        "      |- c\n" +
                        "   |- c\n" +
                        "|- b\n" +
                        "   |- b\n" +
                        "   |- c");
    }

    @Test
    public void testRemove() {
        n = Tree.<Boolean, String>create()
                .setVal(path("a"), true)
                .setVal(path("a", "b", "c"), false)
                .setVal(path("a", "b"), null)
        ;

        assertEquals(1, n.getRoot().getChildMap().get("a").getChildMap().size());

        n.removeNode(path("a", "b", "c"));
        assertEquals(1, n.getRoot().getChildMap().get("a").getChildMap().size());
        n.removeNode(path("a", "b"));
        assertEquals(0, n.getRoot().getChildMap().get("a").getChildMap().size());
    }

    @Test
    public void testRemoveAndRemoveNullBranch() {
        n = Tree.<Boolean, String>create()
                .setVal(path("a"), true)
                .setVal(path("a", "b", "c"), false)
                .setVal(path("a", "b"), null)
        ;

        assertEquals(1, n.getRoot().getChildMap().get("a").getChildMap().size());

        assertFalse(n.removeNodeClearBranchWithNulls(path("a", "b", "c")));
        assertEquals(0, n.getRoot().getChildMap().get("a").getChildMap().size());
    }

    @Test
    public void commonTest() {
        assertTrue(n.getRoot().getValueOfLastExistingParent(path("a", "b", "c", "d")).get());
        assertTrue(n.getRoot().getValueOfLastExistingParent(path("a", "b", "c")).get());
        assertEquals(n.getRoot().getValueOfLastExistingParent(path("d")), empty());

        assertTrue(n.getVal(path("a")));
        assertTrue(n.getVal(path("a", "b")));
        assertTrue(n.getVal(path("a", "b", "c")));
        assertTrue(n.getVal(path("a", "b", "c"))); //doesn't remove value
        assertTrue(n.getVal(path("a", "c")));
        assertTrue(n.getVal(path("b", "b")));

        assertNull(n.getVal(path()));
        assertNull(n.getVal(path("b")));
        assertNull(n.getVal(path("a", "b", "c", "d")));
        assertNull(n.getVal(path("b", "b", "c", "d")));

        assertTrue(n.removeNode(path("a", "b", "c")));
        assertNull(n.getVal(path("a", "b", "c")));
        assertNull(n.removeNode(path("a", "b", "c")));
        assertTrue(n.getVal(path("a", "b")));

        assertTrue(n.removeNode(path("a")));
        assertNull(n.getVal(path("a")));
        assertNull(n.removeNode(path("a")));
        assertTrue(n.getVal(path("a", "b")));
    }


    @Test
    public void testCountLeafs() {
        assertEquals(4, n.getRoot().getLeafCount());
    }

    @Test
    public void testCountNodes() {
        assertEquals(3, n.getRoot().getNonLeafCount());
    }

    @Test
    public void testCountAll() {
        assertEquals(7, n.getRoot().getAllCount());
    }

    @Test
    public void testGetAllLeafs() {
        assertEquals(4, n.getRoot().getAllLeafs().size());
    }

}
