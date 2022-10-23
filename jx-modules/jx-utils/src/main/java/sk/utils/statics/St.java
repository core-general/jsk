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

import lombok.experimental.Accessors;
import sk.exceptions.NotImplementedException;
import sk.utils.functional.*;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.lang.Character.isLetter;
import static java.lang.Character.isSpaceChar;

@SuppressWarnings({"unused", "SpellCheckingInspection", "WeakerAccess"})
public final class St/*rings*/ {
    private static final String UTF_8 = "UTF-8";
    public static final String hex = "0123456789abcdef";
    public static final String eng = "abcdefghijklmnopqrstuvwxyz";
    public static final String ENG = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String dig = "0123456789";
    public static final String engDig = eng + dig;
    public static final String ENGDig = ENG + dig;
    public static final String engENGDig = ENG + eng + dig;

    public static final Charset UTF8 = StandardCharsets.UTF_8;

    //region Common
    private static final long[] orders = LongStream.iterate(1, curVal -> curVal * 10).limit(19).toArray();
    public static final String[] STANDARD_MEASUREMENTS = {"k", "m", "b", "t", "q", "i"};
    public static final String[] MEMORY_MEASUREMENTS = {"Kb", "Mb", "Gb", "Tb", "Pb", "Eb"};

    public static String shortNumberForm(long number) {
        return shortNumberForm(number, STANDARD_MEASUREMENTS);
    }

    public static String shortNumberForm(long number, String[] measurements) {
        if (measurements.length < 6) {
            final String[] arr = new String[6];
            Arrays.fill(arr, "?");
            System.arraycopy(measurements, 0, arr, 0, measurements.length);
            measurements = arr;
        }
        if (number < 0 || number > 999_999_999_999_999_999L) {
            throw new RuntimeException("Unsupported number:" + number);
        }
        if (number == 0) {
            return "0";
        }
        //find curent order
        int order = 0;
        ORDER_SEARCH:
        for (; order < orders.length; order += 3) {
            if ((number) / orders[order] == 0) {
                order = order - 6;
                break ORDER_SEARCH;
            }
        }

        if (order < 0) {
            return String.valueOf(number);
        }

        String suffix = switch (order) {
            case 0 -> measurements[0];
            case 3 -> measurements[1];
            case 6 -> measurements[2];
            case 9 -> measurements[3];
            case 12 -> measurements[4];
            case 15 -> measurements[5];
            default -> throw new IllegalStateException("Unexpected value: " + order);
        };

        final long high = number / orders[order + 3];

        if (number < orders[order + 3]) {
            return high + suffix;
        } else if (number < orders[Math.min(order + 5, orders.length)]) {
            final long low = (number % orders[order + 3]) / orders[order + 2];
            StringBuilder toRet = new StringBuilder(5);
            toRet.append(high);
            if (low > 0) {
                toRet.append(".").append(low);
            }
            toRet.append(suffix);
            return toRet.toString();
        } else {
            return high + suffix;
        }
    }

    public static String removeLastIf(String str, char toRemoveIfLast) {
        if (str.charAt(str.length() - 1) == toRemoveIfLast) {
            return ss(str, 0, str.length() - 1);
        } else {
            return str;
        }
    }

    public static String repeat(String value, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(value);
        }
        return sb.toString();
    }

    public static String repeat(F0<String> value, int times) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < times; i++) {
            str.append(value.get());
        }
        return str.toString();
    }

    public static void forEachChar(String str, C1Char ch) {
        for (int i = 0; i < str.length(); i++) {
            ch.accept(str.charAt(i));
        }
    }

    public static void forEachCodePoint(String str, C1Int ch) {
        str.codePoints().forEach(ch::accept);
    }

    public static String capFirst(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static int count(String string, String toCount) {
        int sum = 0;
        int i = string.indexOf(toCount);
        while (i >= 0) {
            sum++;
            i = string.indexOf(toCount, i + toCount.length());
        }
        return sum;
    }

    public static boolean contains(String str, Pattern regex) {
        return regex.matcher(str).find();
    }

    public static O<String> matchFirst(String str, Pattern regex) {
        Matcher matcher = regex.matcher(str);
        if (matcher.find()) {
            return O.of(matcher.group(1));
        }
        return O.empty();
    }

    public static List<String> matchAllFirst(String str, Pattern regex) {
        Matcher matcher = regex.matcher(str);
        List<String> toRet = new ArrayList<>();
        while (matcher.find()) {
            toRet.add(matcher.group(1));
        }
        return toRet;
    }

    public static List<String> matchFirstAll(String str, Pattern regex) {
        Matcher matcher = regex.matcher(str);
        List<String> lst = new ArrayList<>();
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                lst.add(matcher.group(i));
            }
        }
        return lst;
    }

    public static List<List<String>> matchAll(String str, Pattern regex) {
        Matcher matcher = regex.matcher(str);
        List<List<String>> toRet = new ArrayList<>();
        while (matcher.find()) {
            List<String> lst = new ArrayList<>();
            toRet.add(lst);
            for (int i = 1; i <= matcher.groupCount(); i++) {
                lst.add(matcher.group(i));
            }
        }
        return toRet;
    }

    public static String leaveOnlyLetters(String toProcess) {
        return leaveOnlyLetters(toProcess, false);
    }

    public static String leaveOnlyLetters(String toProcess, boolean spaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toProcess.length(); i++) {
            char c = toProcess.charAt(i);
            if (isLetter(c) || (spaces && isSpaceChar(c))) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static List<String> splitBySize(String toSplit, int maxLineCount) {
        int splits = toSplit.length() / maxLineCount + 1;
        return IntStream.range(0, splits)
                .mapToObj($ -> St.ss(toSplit, maxLineCount * $, maxLineCount * ($ + 1)))
                .filter($ -> !$.isEmpty())
                .collect(Cc.toL());
    }

    public static String fromIntArray(int[] actuallyChars) {
        char[] chars = new char[actuallyChars.length];
        for (int i = 0; i < actuallyChars.length; i++) {
            chars[i] = (char) actuallyChars[i];
        }
        return new String(chars);
    }

    public static String convertIfBigger(String init, int limit, F1<String, String> convertIfLonger) {
        return init.length() > limit ? convertIfLonger.apply(init) : init;
    }

    public static String convertIfSmaller(String init, int limit, F1<String, String> convertIfShorter) {
        return init.length() < limit ? convertIfShorter.apply(init) : init;
    }

    public static String minSymbolsOtherwisePrefix(String num, int minSymbols, String prefix) {
        return convertIfSmaller(num, minSymbols, s -> St.repeat(prefix, minSymbols - s.length()) + num);
    }

    public static String minSymbolsOtherwiseSuffix(String num, int minSymbols, String prefix) {
        return convertIfSmaller(num, minSymbols, s -> num + St.repeat(prefix, minSymbols - s.length()));
    }

    public static String raze(String init, int limit) {
        return convertIfBigger(init, limit, s -> St.ss(init, 0, limit));
    }

    public static String raze3dots(String init, int limit) {
        return convertIfBigger(init, limit, s -> St.ss(init, 0, limit - 3) + "...");
    }

    public static boolean isNullOrEmpty(String someVal) {
        return someVal == null || someVal.isEmpty();
    }

    public static boolean isNotNullOrEmpty(String someVal) {
        return !isNullOrEmpty(someVal);
    }

    public static String addTabsLeft(String text, int tabCount) {
        final String repeat = St.repeat("   ", tabCount);
        return repeat + text.replace("\n", "\n" + repeat);
    }

    public static int levenshteinDistance(String s1, String s2) {
        return dist(s1.toCharArray(), s2.toCharArray());
    }

    public static O<String> longestCommonSubstring(String x, String y) {
        return longestCommonSubstringPrivate(x, y, x.length(), y.length());
    }

    //endregion

    //region Start or end
    public static String startWith(String str, String mustStartWith) {
        if (mustStartWith.length() > 1) {
            throw new NotImplementedException("Not yet :P");
        }
        if (!str.startsWith(mustStartWith)) {
            str = mustStartWith + str;
        }
        return str;
    }

    public static String endWith(String str, String mustEndWith) {
        if (mustEndWith.length() > 1) {
            throw new NotImplementedException("Not yet :P");
        }
        if (!str.endsWith(mustEndWith)) {
            str = str + mustEndWith;
        }
        return str;
    }

    public static String notStartWith(String str, String notStartWith) {
        if (notStartWith.length() > 1) {
            throw new NotImplementedException("Not yet :P");
        }
        if (str.startsWith(notStartWith)) {
            str = St.ss(str, 1, str.length());
        }
        return str;
    }

    public static String notEndWith(String str, String notEndWith) {
        if (notEndWith.length() > 1) {
            throw new NotImplementedException("Not yet :P");
        }
        if (str.endsWith(notEndWith)) {
            str = St.ss(str, 0, str.length() - 1);
        }
        return str;
    }

    //endregion

    //region substring operations

    //substring
    public static String ss(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        if (start < 0) {
            start = 0;
        }
        if (end >= str.length() || end == -1) {
            end = str.length();
        }
        if (start >= str.length()) {
            start = str.length();
        }
        if (end <= start || "".equals(str)) {
            return "";
        }
        return str.substring(start, end);
    }

    public static SS sub(String str) {
        return new SS(str);
    }

    public static SS sub(String str, String left, String right) {
        return new SS(str).leftFirst(left).rightLast(right);
    }

    public static String subLF(String str, String left) {
        return new SS(str).leftFirst(left).get();
    }

    public static String subRF(String str, String right) {
        return new SS(str).rightFirst(right).get();
    }

    public static String subLL(String str, String left) {
        return new SS(str).leftLast(left).get();
    }

    public static String subRL(String str, String right) {
        return new SS(str).rightLast(right).get();
    }

    //endregion

    //region byte[] and streams
    public static byte[] utf8(String str) {
        return bytes(str, UTF_8);
    }

    public static byte[] bytes(String str, String encoding) {
        if (str == null) {
            return null;
        }
        try {
            return str.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return Ex.thRow(e);
        }
    }

    public static String bytesToS(byte[] bytes) {
        return bytesToS(bytes, UTF_8);
    }

    public static String bytesToS(byte[] bytes, String charSet) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, charSet);
        } catch (UnsupportedEncodingException e) {
            return Ex.thRow(e);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    public static byte[] hexToBytes(String hexString) {
        return HexFormat.of().parseHex(hexString);
    }

    public static String streamToS(InputStream is) {
        return streamToS(is, UTF_8);
    }

    public static String streamToS(InputStream is, String charSet) {
        return bytesToS(Io.streamToBytes(is), charSet);
    }

    //endregion

    //region private
    private static int dist(char[] s1, char[] s2) {

        // distance matrix - to memoize distances between substrings
        // needed to avoid recursion
        int[][] d = new int[s1.length + 1][s2.length + 1];

        // d[i][j] - would contain distance between such substrings:
        // s1.subString(0, i) and s2.subString(0, j)

        for (int i = 0; i < s1.length + 1; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j < s2.length + 1; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i < s1.length + 1; i++) {
            for (int j = 1; j < s2.length + 1; j++) {
                int d1 = d[i - 1][j] + 1;
                int d2 = d[i][j - 1] + 1;
                int d3 = d[i - 1][j - 1];
                if (s1[i - 1] != s2[j - 1]) {
                    d3 += 1;
                }
                d[i][j] = Math.min(Math.min(d1, d2), d3);
            }
        }
        return d[s1.length][s2.length];
    }

    private static O<String> longestCommonSubstringPrivate(String x, String y, int m, int n) {
        // Create a table to store lengths of longest common
        // suffixes of substrings.   Note that LCSuff[i][j]
        // contains length of longest common suffix of X[0..i-1]
        // and Y[0..j-1]. The first row and first column entries
        // have no logical meaning, they are used only for
        // simplicity of program
        int[][] LCSuff = new int[m + 1][n + 1];

        // To store length of the longest common substring
        int len = 0;

        // To store the index of the cell which contains the
        // maximum value. This cell's index helps in building
        // up the longest common substring from right to left.
        int row = 0, col = 0;

        /* Following steps build LCSuff[m+1][n+1] in bottom
           up fashion. */
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {LCSuff[i][j] = 0;} else if (x.charAt(i - 1) == y.charAt(j - 1)) {
                    LCSuff[i][j] = LCSuff[i - 1][j - 1] + 1;
                    if (len < LCSuff[i][j]) {
                        len = LCSuff[i][j];
                        row = i;
                        col = j;
                    }
                } else {LCSuff[i][j] = 0;}
            }
        }

        // if true, then no common substring exists
        if (len == 0) {
            return O.empty();
        }

        // allocate space for the longest common substring
        String resultStr = "";

        // traverse up diagonally form the (row, col) cell
        // until LCSuff[row][col] != 0
        while (LCSuff[row][col] != 0) {
            resultStr = x.charAt(row - 1) + resultStr; // or Y[col-1]
            --len;

            // move diagonally up to previous cell
            row--;
            col--;
        }
        return O.of(resultStr);
    }

    @Accessors(chain = true, fluent = true)
    public static class SS {
        final private String str;
        LRProcessor left = new LRProcessor() {
            @Override
            protected int difIndex() {
                return (valIncluded ? 0 : val.length());
            }
        };
        LRProcessor right = new LRProcessor() {
            @Override
            protected int difIndex() {
                return (valIncluded ? val.length() : 0);
            }
        };

        private abstract static class LRProcessor {
            protected String val;
            protected boolean valLast = false;
            protected boolean valIncluded = false;

            protected int index(String str, int index) {
                if (val != null) {
                    int v = valLast ? str.lastIndexOf(val) : str.indexOf(val);
                    if (v > -1) {
                        index = v + difIndex();
                    }
                }
                return index;
            }

            protected abstract int difIndex();
        }

        public SS(String str) {
            this.str = str;
        }

        public SS leftIncluded() {
            left.valIncluded = true;
            return this;
        }

        public SS rightIncluded() {
            right.valIncluded = true;
            return this;
        }

        public SS leftLast(String left) {
            this.left.valLast = true;
            this.left.val = left;
            return this;
        }

        public SS leftFirst(String left) {
            this.left.valLast = false;
            this.left.val = left;
            return this;
        }

        public SS rightFirst(String right) {
            this.right.valLast = false;
            this.right.val = right;
            return this;
        }

        public SS rightLast(String right) {
            this.right.valLast = true;
            this.right.val = right;
            return this;
        }

        public String get() {
            String st = str;
            int leftIndex = left.index(st, 0);
            if (leftIndex > 0) {
                st = st.substring(leftIndex);
            }
            int rightIndex = right.index(st, st.length());
            return ss(st, 0, rightIndex);
        }

        @Override
        public String toString() {
            return get();
        }
    }

    private St() {
    }
    //endregion
}
