package sk.services.bytes;

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

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import sk.services.http.CrcAndSize;
import sk.services.rand.RandImpl;
import sk.test.MockitoTest;
import sk.utils.statics.Io;
import sk.utils.statics.St;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BytesImplTest extends MockitoTest {
    @InjectMocks BytesImpl bytes;
    @InjectMocks RandImpl rnd;

    @Test
    public void crcAnyTest() {
        assertEquals("a9707064", bytes.crcAny(new byte[]{0}, 4).asHex());
        assertEquals("c2a4af3a", bytes.crcAny(new byte[]{1}, 4).asHex());
        assertEquals("a97070643714e5c1", bytes.crcAny(new byte[]{0}, 8).asHex());
        assertEquals("a97070643714e5c1c013d557591a84ed", bytes.crcAny(new byte[]{0}, 16).asHex());
        assertEquals("a97070643714e5c1c013d557591a84edae1db47b7cec5f1e8beb6f8c1b54721b", bytes.crcAny(new byte[]{0}, 32).asHex());

        assertEquals("36NktI", bytes.crcAny(new byte[]{0}, 4).asBase62());
        assertEquals("3Z00cs", bytes.crcAny(new byte[]{1}, 4).asBase62());
        assertEquals("EXv6Bp7QZwf", bytes.crcAny(new byte[]{0}, 8).asBase62());
        assertEquals("59j5SQYqH1iqvv23viuqlR", bytes.crcAny(new byte[]{0}, 16).asBase62());
        assertEquals("eB73qfjkxeOL0VHkU1tkVXZiIc1kGVXOF4zWP3Ch0zD", bytes.crcAny(new byte[]{0}, 32).asBase62());

        assertEquals("7dfbf474-f187-30a6-9be0-1ef6b79104a5", bytes.crcAny(new byte[]{0}, 16).asUUID().toString());
        assertEquals("5803c742-ad21-3ec2-894c-d02af720649b", bytes.crcAny(new byte[]{1}, 16).asUUID().toString());
    }

    @Test
    public void decodeCrcEncodedValueTest() {
        IntStream.range(0, 1_000).parallel().forEach($ -> {
            final String val = rnd.rndString(rnd.rndInt(1, 5), St.engENGDig);
            final long crc = bytes.crc32(val.getBytes(StandardCharsets.UTF_8));
            final String s = bytes.decodeCrcEncodedValue(crc + val)
                    .orElseThrow(() -> new RuntimeException("Wrong: " + crc + val));
            assertEquals(s, val, "Wrong: " + crc + val);
        });
    }

    @Test
    public void crc32Stream() {
        byte[] buf = {1, 2, 3, 4, 5};
        InputStream is = new ByteArrayInputStream(buf);
        CrcAndSize crcAndSize = bytes.crc32(is, 2, Io.NONE);
        long l = bytes.crc32(buf);
        assertEquals(new CrcAndSize(l, buf.length), crcAndSize);
    }
}
