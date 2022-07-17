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

public class CsvReaderTest {

    @Test
    public void getFromCsv() {
        {
            String csv = """
                    a, b , "c" , d, e, f
                    1 , "23""4","567"    ,8,,
                    """;
            assertEquals(Cc.m("a", "1", "b", "23\"4", "c", "567", "d", "8", "e", null, "f", null),
                    CsvReader.getFromCsv(csv).get(0));
        }

        {
            String badCsv = """
                    a, b , c , d
                    1 , "23""4"a ,"567"    ,8
                    """;
            assertThrows(RuntimeException.class, () -> CsvReader.getFromCsv(badCsv));
        }
        {
            String badCsv2 = """
                    a, b , c , d
                    1 , "23""4" ,a"567"    ,8
                    """;
            assertThrows(RuntimeException.class, () -> CsvReader.getFromCsv(badCsv2));
        }
        {
            String badCsv2 = """
                    a, b , c , d
                    1 , "23""4" , "567"   a ,8
                    """;
            assertThrows(RuntimeException.class, () -> CsvReader.getFromCsv(badCsv2));
        }
        {
            String badCsv2 = """
                    a, b , c , d
                    1 , "23""4" , "567"    ,"8
                    """;
            assertThrows(RuntimeException.class, () -> CsvReader.getFromCsv(badCsv2));
        }

        {
            String badCsv2 = """
                    a, b , c , ,
                    1 , "23""4" , "567"    ,8
                    """;
            assertThrows(RuntimeException.class, () -> CsvReader.getFromCsv(badCsv2));
        }
    }
}
