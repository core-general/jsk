package sk.utils.javafixes;

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
/*
MIT License

Copyright (c) 2017 Sebastian Ruhleder

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
A Base62 encoder/decoder.

@author Sebastian Ruhleder, sebastian@seruco.io

Slightly modified
*/

import java.io.ByteArrayOutputStream;

import static sk.utils.statics.St.UTF8;

public class Base62 {
    private static final byte[] alphabet = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9',

            (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F',
            (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
            (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V',
            (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',

            (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',
            (byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l',
            (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't',
            (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z'
    };

    private final static int TARGET_BASE = alphabet.length;
    private static final int STANDARD_BASE = 256;
    private final static byte[] lookup = new byte[STANDARD_BASE];

    static {
        for (int i = 0; i < alphabet.length; i++) {
            lookup[alphabet[i]] = (byte) (i & 0xFF);
        }
    }

    /**
     * Encodes a sequence of bytes in Base62 encoding.
     *
     * @param message a byte sequence.
     * @return a sequence of Base62-encoded bytes.
     */
    public static String encode(final byte[] message) {
        final byte[] indices = convert(message, STANDARD_BASE, TARGET_BASE);

        return new String(translate(indices, alphabet), UTF8);
    }

    public static String encodeStr(final String message) {
        return encode(message.getBytes(UTF8));
    }


    /**
     * Decodes a sequence of Base62-encoded bytes.
     *
     * @param encoded a sequence of Base62-encoded bytes.
     * @return a byte sequence.
     * @throws IllegalArgumentException when {@code encoded} is not encoded over the Base62 alphabet.
     */
    public static byte[] decode(final String encoded) {
        byte[] bytez = encoded.getBytes(UTF8);
        if (!isBase62Encoding(bytez)) {
            throw new IllegalArgumentException("Input is not encoded correctly");
        }

        final byte[] prepared = translate(bytez, lookup);

        return convert(prepared, TARGET_BASE, STANDARD_BASE);
    }

    public static String decodeStr(final String encoded) {
        return new String(decode(encoded), UTF8);
    }

    /**
     * Checks whether a sequence of bytes is encoded over a Base62 alphabet.
     *
     * @param bytes a sequence of bytes.
     * @return {@code true} when the bytes are encoded over a Base62 alphabet, {@code false} otherwise.
     */
    public static boolean isBase62Encoding(final byte[] bytes) {
        if (bytes == null) {
            return false;
        }

        for (final byte e : bytes) {
            if ('0' > e || '9' < e) {
                if ('a' > e || 'z' < e) {
                    if ('A' > e || 'Z' < e) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Uses the elements of a byte array as indices to a dictionary and returns the corresponding values
     * in form of a byte array.
     */
    private static byte[] translate(final byte[] indices, final byte[] dictionary) {
        final byte[] translation = new byte[indices.length];

        for (int i = 0; i < indices.length; i++) {
            translation[i] = dictionary[indices[i]];
        }

        return translation;
    }

    /**
     * Converts a byte array from a source base to a target base using the alphabet.
     */
    private static byte[] convert(final byte[] message, final int sourceBase, final int targetBase) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(estimateOutputLength(message.length, sourceBase, targetBase));
        byte[] source = message;
        while (source.length > 0) {
            final ByteArrayOutputStream quotient = new ByteArrayOutputStream(source.length);
            int remainder = 0;

            for (int i = 0; i < source.length; i++) {
                final int accumulator = (source[i] & 0xFF) + remainder * sourceBase;
                final int digit = (accumulator - (accumulator % targetBase)) / targetBase;
                remainder = accumulator % targetBase;
                if (quotient.size() > 0 || digit > 0) {
                    quotient.write(digit);
                }
            }
            out.write(remainder);
            source = quotient.toByteArray();
        }

        // pad output with zeroes corresponding to the number of leading zeroes in the message
        for (int i = 0; i < message.length - 1 && message[i] == 0; i++) {
            out.write(0);
        }

        return reverse(out.toByteArray());
    }

    /**
     * Estimates the length of the output in bytes.
     */
    private static int estimateOutputLength(int inputLength, int sourceBase, int targetBase) {
        return (int) Math.ceil((Math.log(sourceBase) / Math.log(targetBase)) * inputLength);
    }

    /**
     * Reverses a byte array.
     */
    private static byte[] reverse(final byte[] arr) {
        final int length = arr.length;

        final byte[] reversed = new byte[length];

        for (int i = 0; i < length; i++) {
            reversed[length - i - 1] = arr[i];
        }

        return reversed;
    }
}

