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

import org.junit.Test;
import org.mockito.InjectMocks;
import sk.services.rand.RandImpl;
import sk.test.MockitoTest;
import sk.utils.statics.St;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class BytesImplTest extends MockitoTest {
    @InjectMocks BytesImpl bytes;
    @InjectMocks RandImpl rnd;

    @Test
    public void decodeCrcEncodedValueTest() {
        IntStream.range(0, 1_000).parallel().forEach($ -> {
            final String val = rnd.rndString(rnd.rndInt(1, 5), St.engENGDig);
            final long crc = bytes.crc32(val.getBytes(StandardCharsets.UTF_8));
            final String s = bytes.decodeCrcEncodedValue(crc + val)
                    .orElseThrow(() -> new RuntimeException("Wrong: " + crc + val));
            assertEquals("Wrong: " + crc + val, s, val);
        });
    }
}
