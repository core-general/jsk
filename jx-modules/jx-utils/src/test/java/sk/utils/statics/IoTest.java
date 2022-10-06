package sk.utils.statics;

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

import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;

public class IoTest {

    @Test
    public void streamToBytes() {
        final int limit = 10_000;
        byte[] bytes = new byte[limit];
        for (int i = 0; i < limit; i++) {
            bytes[i] = (byte) (i % 256);
        }

        final InputStream stream = Io.bytesToStream(bytes);

        final byte[] bytes1 = Io.streamToBytes(stream);
        assertArrayEquals(bytes, bytes1);
    }
}
