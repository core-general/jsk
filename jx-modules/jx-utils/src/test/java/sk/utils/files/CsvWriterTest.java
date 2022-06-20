package sk.utils.files;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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
import sk.utils.statics.Cc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class CsvWriterTest {

    @Test
    public void toCsv() {
        {
            final String s = CsvWriter.toCsv(
                    Cc.l(
                            Cc.m("a", 1, "c", 3, "d", 4, "b", 2),
                            Cc.m("a", 1, "b", 2, "d", 4, "c", 3),
                            Cc.m("a", 1, "d", 4)
                    ),
                    Cc.l("a", "b", "c", "d"),
                    ","
            );

            assertEquals("a,b,c,d\n" +
                    "1,2,3,4\n" +
                    "1,2,3,4\n" +
                    "1,,,4", s);
        }

        {
            final String s = CsvWriter.toCsv(
                    Cc.l(
                            Cc.m("a", 1, "c", 3, "d", 4, "b", 2),
                            Cc.m("a", 1, "b", 2, "d", 4, "c", 3),
                            Cc.m("a", 1, "d", 4)
                    )
            );

            assertEquals("a,b,c,d\n" +
                    "1,2,3,4\n" +
                    "1,2,3,4\n" +
                    "1,,,4", s);
        }


        {
            assertThrows(RuntimeException.class, () -> CsvWriter.toCsv(
                    Cc.l(
                            Cc.m("a", 1, "c", 3, "d", 4, "b", 2),
                            Cc.m("a", 1, "b", 2, "d", 4, "c", 3),
                            Cc.m("a", 1, "d", 4)
                    ),
                    Cc.l("a", "b", "c"),
                    ","
            ));
        }
    }
}
