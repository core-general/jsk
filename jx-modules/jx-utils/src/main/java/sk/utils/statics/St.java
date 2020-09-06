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

import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import sk.exceptions.NotImplementedException;
import sk.utils.functional.C1Char;
import sk.utils.functional.F0;
import sk.utils.functional.F1;
import sk.utils.functional.O;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.lang.Character.isLetter;
import static java.lang.Character.isSpaceChar;

@SuppressWarnings({"unused", "SpellCheckingInspection", "WeakerAccess"})
public final class St {
    private static final String UTF_8 = "UTF-8";
    final private static char[] hexArray = "0123456789abcdef".toCharArray();
    public static final String eng = "abcdefghijklmnopqrstuvwxyz";
    public static final String ENG = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String dig = "0123456789";
    public static final String engDig = eng + dig;
    public static final String ENGDig = ENG + dig;
    public static final String engENGDig = ENG + eng + dig;

    //region Common
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

    @SneakyThrows
    public static byte[] bytes(String str, String encoding) {
        if (str == null) {
            return null;
        }
        return str.getBytes(encoding);
    }

    public static String bytesToS(byte[] bytes) {
        return bytesToS(bytes, UTF_8);
    }

    @SneakyThrows
    public static String bytesToS(byte[] bytes, String charSet) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, charSet);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexes = new char[bytes.length * 2];
        int bound = bytes.length;
        for (int j = 0; j < bound; j++) {
            int v = bytes[j] & 0xFF;
            hexes[j * 2] = hexArray[v >>> 4];
            hexes[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexes);
    }

    public static String streamToS(InputStream is) {
        return streamToS(is, UTF_8);
    }

    @SneakyThrows
    public static String streamToS(InputStream is, String charSet) {
        return bytesToS(Io.streamToBytes(is), charSet);
    }

    //endregion

    //region private

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
