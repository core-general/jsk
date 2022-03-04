package sk.utils.statics;

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
import sk.utils.functional.O;

import static org.junit.Assert.assertEquals;

public class StTest {

    @Test
    public void count() {
        final String str = "abc;def,ghk;xx,bb;ioo;";
        assertEquals(St.count(str, ";"), 4);
        assertEquals(St.count(str, " "), 0);
        assertEquals(St.count(str, "oo"), 1);
    }

    @Test
    public void minSymbolsOtherwisePrefixTest() {
        assertEquals(St.minSymbolsOtherwisePrefix("1", 2, "0"), "01");
        assertEquals(St.minSymbolsOtherwisePrefix("1", 3, "0"), "001");
        assertEquals(St.minSymbolsOtherwisePrefix("555", 3, "0"), "555");
    }

    @Test
    public void sub() {
        final String str = "abc;def,ghk;xx,bb;ioo;";
        assertEquals(St.sub(str).leftFirst(";").rightFirst(";").get(), "def,ghk");
        assertEquals(St.sub(str).leftFirst(";").rightLast(";").get(), "def,ghk;xx,bb;ioo");
        assertEquals(St.sub(str).leftLast(",").rightFirst(";").get(), "bb");
        assertEquals(St.sub(str).leftLast(",").rightLast(";").get(), "bb;ioo");
    }

    @Test
    public void subOneLine() {
        final String str = "http://abc.com/bde";
        assertEquals(St.subLF(str, "/"), "/abc.com/bde");
        assertEquals(St.subLL(str, "/"), "bde");
        assertEquals(St.subRF(str, "/"), "http:");
        assertEquals(St.subRL(str, "/"), "http://abc.com");
        assertEquals(St.subRL(str, "/"), "http://abc.com");
    }

    @Test
    public void levenshteinDistance() {
        assertEquals(St.levenshteinDistance("Kivan", "ivakuator"), 7);
    }

    @Test
    public void longestCommonSubstring() {
        assertEquals(St.longestCommonSubstring("kivan", "ivakuator").get(), "iva");
        assertEquals(St.longestCommonSubstring("kivan", "ivakivauator").get(), "kiva");
        assertEquals(St.longestCommonSubstring("abc", "def"), O.empty());
    }
}
