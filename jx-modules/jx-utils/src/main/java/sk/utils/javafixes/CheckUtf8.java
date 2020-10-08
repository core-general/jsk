package sk.utils.javafixes;

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

/**
 * Taken from https://codereview.stackexchange.com/questions/59428/validating-utf-8-byte-array
 */
public class CheckUtf8 {

    /**
     * Returns the number of UTF-8 characters, or -1 if the array does not
     * contain a valid UTF-8 string. Overlong encodings, null characters,
     * invalid Unicode values, and surrogates are accepted.
     *
     * @param bytes byte array to check length
     * @return length
     */
    public static int countUtf8Chars(byte[] bytes) {
        int charCount = 0, expectedLen;

        for (int i = 0; i < bytes.length; i++) {
            charCount++;
            // Lead byte analysis
            if ((bytes[i] & 0b10000000) == 0b00000000) {
                continue;
            } else if ((bytes[i] & 0b11100000) == 0b11000000) {
                expectedLen = 2;
            } else if ((bytes[i] & 0b11110000) == 0b11100000) {
                expectedLen = 3;
            } else if ((bytes[i] & 0b11111000) == 0b11110000) {
                expectedLen = 4;
            } else if ((bytes[i] & 0b11111100) == 0b11111000) {
                expectedLen = 5;
            } else if ((bytes[i] & 0b11111110) == 0b11111100) {
                expectedLen = 6;
            } else {
                return -1;
            }

            // Count trailing bytes
            while (--expectedLen > 0) {
                if (++i >= bytes.length) {
                    return -1;
                }
                if ((bytes[i] & 0b11000000) != 0b10000000) {
                    return -1;
                }
            }
        }
        return charCount;
    }

    /**
     * Validate a UTF-8 byte array
     *
     * @param bytes byte array to isUtf8String
     * @return true if UTF-8
     */
    public static boolean isUtf8String(byte[] bytes) {
        return (countUtf8Chars(bytes) != -1);
    }

    public static boolean isBinary(byte[] bytes) {
        return !isUtf8String(bytes);
    }
}
