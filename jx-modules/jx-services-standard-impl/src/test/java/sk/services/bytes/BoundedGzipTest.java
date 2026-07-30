package sk.services.bytes;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

import org.junit.jupiter.api.Test;
import sk.utils.statics.Io;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedGzipTest {
    private final BytesImpl bytes = new BytesImpl();

    @Test
    void acceptsOneExactSingleMember() {
        byte[] input = new byte[49_152];
        Arrays.fill(input, (byte) 7);
        byte[] gzip = bytes.gzip(input);

        assertArrayEquals(input, bytes.unGzipBytes(gzip, input.length));
    }

    @Test
    void stopsACompressionBombAtTheConfiguredOutputLimit() {
        byte[] input = new byte[2_000_000];
        byte[] gzip = bytes.gzip(input);

        assertThrows(
                Io.StreamLimitExceededException.class,
                () -> bytes.unGzipBytes(gzip, 49_152));
    }

    @Test
    void rejectsConcatenatedAndTrailingMembers() {
        byte[] first = bytes.gzip(new byte[]{1, 2, 3});
        byte[] second = bytes.gzip(new byte[]{4, 5, 6});
        byte[] concatenated = new byte[first.length + second.length];
        System.arraycopy(first, 0, concatenated, 0, first.length);
        System.arraycopy(second, 0, concatenated, first.length, second.length);
        byte[] trailing = Arrays.copyOf(first, first.length + 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> bytes.unGzipBytes(concatenated, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> bytes.unGzipBytes(trailing, 100));
    }

    @Test
    void rejectsTruncationAndCorruptTrailers() {
        byte[] gzip = bytes.gzip(new byte[]{1, 2, 3});
        byte[] truncated = Arrays.copyOf(gzip, gzip.length - 1);
        byte[] corruptCrc = gzip.clone();
        corruptCrc[corruptCrc.length - 8] ^= 1;
        byte[] corruptSize = gzip.clone();
        corruptSize[corruptSize.length - 4] ^= 1;

        assertThrows(
                IllegalArgumentException.class,
                () -> bytes.unGzipBytes(truncated, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> bytes.unGzipBytes(corruptCrc, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> bytes.unGzipBytes(corruptSize, 100));
    }

    @Test
    void rejectsReservedHeaderFlags() {
        byte[] gzip = bytes.gzip(new byte[]{1});
        gzip[3] = (byte) 0xe0;

        assertThrows(
                IllegalArgumentException.class,
                () -> bytes.unGzipBytes(gzip, 100));
    }
}
