package sk.utils.statics;

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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static sk.utils.statics.Ex.*;

public class ExTest extends MockitoTest {

    @Test
    public void thRowTest() {
        try {
            thRow();
            fail();
        } catch (Exception ignored) {
        }
    }

    @Test
    public void traceAsStringTest() {
        assertTrue(traceAsString(new RuntimeException()).contains("ExTest"));
    }

    @Test
    public void getInfoTest() {
        String hi = getInfo(new RuntimeException("Hi"));
        assertTrue(hi.contains("Hi"));
        assertTrue(hi.contains("ExTest"));
    }

    @Test
    public void getThrowExceptionTest() {
    }

    @Test
    public void getIgnoreExceptionTest() {
    }

    @Test
    public void runIgnoreExceptionTest() {
    }

    @Test
    public void runThrowExceptionTest() {
    }
}
