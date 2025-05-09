package sk.utils.files;

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

import org.junit.jupiter.api.Test;
import sk.utils.functional.O;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathWithBaseTest {

    @Test
    public void addToPath() {
        PathWithBase pwb = new PathWithBase("A");
        assertEquals(new PathWithBase("A"), pwb);
        assertEquals("a", (pwb = pwb.addToPath("a")).getPathNoSlash());
        assertEquals("a/b", (pwb = pwb.addToPath("b")).getPathNoSlash());
        assertEquals("a/b/c", (pwb = pwb.addToPath("/c")).getPathNoSlash());
        assertEquals("a/b/c/d/e", (pwb = pwb.addToPath("d/e")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f", (pwb = pwb.addToPath("f/")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f/g", (pwb = pwb.addToPath("/g")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f/g/h/", (pwb = pwb.addToPath("h")).getPathWithSlash());

        pwb = new PathWithBase("A/a");
        assertEquals(new PathWithBase("A/a"), pwb);
        assertEquals("a/b", (pwb = pwb.addToPath("b")).getPathNoSlash());
        assertEquals("a/b/c", (pwb = pwb.addToPath("/c")).getPathNoSlash());
        assertEquals("a/b/c/d/e", (pwb = pwb.addToPath("d/e")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f", (pwb = pwb.addToPath("f/")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f/g", (pwb = pwb.addToPath("/g")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f/g/h/", (pwb = pwb.addToPath("h")).getPathWithSlash());

        pwb = new PathWithBase("A", "a");
        assertEquals(new PathWithBase("A/a"), pwb);
        assertEquals("a/b", (pwb = pwb.addToPath("b")).getPathNoSlash());
        assertEquals("a/b/c", (pwb = pwb.addToPath("/c")).getPathNoSlash());
        assertEquals("a/b/c/d/e", (pwb = pwb.addToPath("d/e")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f", (pwb = pwb.addToPath("f/")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f/g", (pwb = pwb.addToPath("/g")).getPathNoSlash());
        assertEquals("a/b/c/d/e/f/g/h/", (pwb = pwb.addToPath("h")).getPathWithSlash());

        pwb = new PathWithBase("A/a", "a");
        assertEquals(new PathWithBase("A/a/a"), pwb);
        assertEquals("a/a/b", (pwb = pwb.addToPath("b")).getPathNoSlash());
        assertEquals("a/a/b/c", (pwb = pwb.addToPath("/c")).getPathNoSlash());
        assertEquals("a/a/b/c/d/e", (pwb = pwb.addToPath("d/e")).getPathNoSlash());
        assertEquals("a/a/b/c/d/e/f", (pwb = pwb.addToPath("f/")).getPathNoSlash());
        assertEquals("a/a/b/c/d/e/f/g", (pwb = pwb.addToPath("/g")).getPathNoSlash());
        assertEquals("a/a/b/c/d/e/f/g/h/", (pwb = pwb.addToPath("h")).getPathWithSlash());
    }

    @Test
    public void getParent() {
        assertEquals(new PathWithBase("A").getParent(), O.empty());
        assertEquals(new PathWithBase("A", "").getParent(), O.empty());
        assertEquals(new PathWithBase("A", "/").getParent(), O.empty());
        assertEquals(new PathWithBase("A", "B").getParent(), O.of(new PathWithBase("A")));
        assertEquals(new PathWithBase("A", "/B").getParent(), O.of(new PathWithBase("A")));
        assertEquals(new PathWithBase("A", "B/").getParent(), O.of(new PathWithBase("A")));
        assertEquals(new PathWithBase("A", "B/C").getParent(), O.of(new PathWithBase("A", "B")));
        assertEquals(new PathWithBase("A", "B/C/").getParent(), O.of(new PathWithBase("A", "B")));
        assertEquals(new PathWithBase("A", "B/C/D").getParent(), O.of(new PathWithBase("A", "B/C")));
    }
}
