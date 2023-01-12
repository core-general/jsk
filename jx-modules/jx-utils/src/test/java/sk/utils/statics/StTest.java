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

import org.junit.Assert;
import org.junit.Test;
import sk.utils.functional.O;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class StTest {
    @Test
    public void hexToBytesAndBack() {
        final byte[] initial = new byte[]{0, 0, 5, 1, 10, 0, 0};

        final String hexed = St.bytesToHex(initial);
        final byte[] back = St.hexToBytes(hexed);
        Assert.assertArrayEquals(initial, back);
    }

    @Test
    public void shortNumberFormTest() {
        assertEquals(St.shortNumberForm(0), "0");
        assertEquals(St.shortNumberForm(1), "1");
        assertEquals(St.shortNumberForm(10), "10");
        assertEquals(St.shortNumberForm(100), "100");
        assertEquals(St.shortNumberForm(999), "999");
        assertEquals(St.shortNumberForm(1000), "1k");
        assertEquals(St.shortNumberForm(1001), "1k");
        assertEquals(St.shortNumberForm(1011), "1k");
        assertEquals(St.shortNumberForm(1111), "1.1k");
        assertEquals(St.shortNumberForm(2111), "2.1k");
        assertEquals(St.shortNumberForm(12111), "12.1k");
        assertEquals(St.shortNumberForm(99999), "99.9k");
        assertEquals(St.shortNumberForm(112111), "112k");
        assertEquals(St.shortNumberForm(1_000_000), "1m");
        assertEquals(St.shortNumberForm(1112111), "1.1m");
        assertEquals(St.shortNumberForm(11112111), "11.1m");
        assertEquals(St.shortNumberForm(111112111), "111m");
        assertEquals(St.shortNumberForm(1111112111), "1.1b");
        assertEquals(St.shortNumberForm(1111111112111L), "1.1t");
        assertEquals(St.shortNumberForm(1111111111112111L), "1.1q");
        assertEquals(St.shortNumberForm(11111111111112111L), "11.1q");
        assertEquals(St.shortNumberForm(111111111111112111L), "111q");
        assertEquals(St.shortNumberForm(999999999999999999L), "999q");
        assertEquals(St.shortNumberForm(999999999999999999L, new String[]{"X", "XX"}), "999?");
        assertEquals(St.shortNumberForm(999999999999999999L, new String[]{"_1", "_2", "_3", "_4", "_5"}), "999_5");
        assertThrows(RuntimeException.class, () -> St.shortNumberForm(1000000000000000000L));
        assertThrows(RuntimeException.class, () -> St.shortNumberForm(-1L));
    }

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
    public void sub2() {
        final String str = "[] avhsdjg \" ajdfhajshf jafjas";
        assertEquals(St.sub(str, "]", "\"").get().trim(), "avhsdjg");
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
        assertEquals(St.levenshteinDistance("Kivan", "Kivan"), 0);
        assertEquals(St.levenshteinDistance("Kivan", "kivan"), 1);
    }

    @Test
    public void longestCommonSubstring() {
        assertEquals(St.longestCommonSubstring("kivan", "ivakuator").get(), "iva");
        assertEquals(St.longestCommonSubstring("kivan", "ivakivauator").get(), "kiva");
        assertEquals(St.longestCommonSubstring("abc", "def"), O.empty());
    }

    @Test
    public void snakeToCamelCase() {
        assertEquals(St.snakeToCamelCase("abc"), "Abc");
        assertEquals(St.snakeToCamelCase("abc_def"), "AbcDef");
        assertEquals(St.snakeToCamelCase("abc_def_geh"), "AbcDefGeh");
        assertEquals(St.snakeToCamelCase("_abc_def_geh_"), "AbcDefGeh");
    }
}
