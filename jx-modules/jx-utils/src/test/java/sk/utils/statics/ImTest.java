package sk.utils.statics;

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

import java.awt.*;

import static org.junit.Assert.assertEquals;

public class ImTest {

    @Test
    public void testToHexColor() {
        assertEquals(Im.toHexColor(Color.BLACK, false), "#000000");
        assertEquals(Im.toHexColor(Color.RED, false), "#ff0000");
        assertEquals(Im.toHexColor(Color.GREEN, false), "#00ff00");
        assertEquals(Im.toHexColor(Color.BLUE, false), "#0000ff");
        assertEquals(Im.toHexColor(Color.WHITE, false), "#ffffff");
        assertEquals(Im.toHexColor(new Color(16, 32, 64, 128), false), "#102040");
        assertEquals(Im.toHexColor(new Color(16, 32, 64, 128), true), "#10204080");
    }
}
