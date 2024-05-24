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

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class IoTest {
    @Test
    public void streamToBytes() {
        final int limit = 10_000;
        byte[] bytes = new byte[limit];
        for (int i = 0; i < limit; i++) {
            bytes[i] = (byte) (i % 256);
        }

        final InputStream stream = Io.bytesToStream(bytes);

        final byte[] bytes1 = Io.streamPump(stream);
        assertArrayEquals(bytes, bytes1);
    }

    @Test
    void changePortForUrlTest() {
        assertEquals("""
                        jdbc:postgresql://eat-db.cywcsttzyjee.us-east-1.rds.amazonaws.com:69543/eat?ssl=true&sslmode=verify-full&sslfactory=org.postgresql.ssl.SingleCertValidatingFactory&sslfactoryarg=classpath:ead/rds-ca-2019-us-east-1.pem""",
                Io.changePortForUrl("""
                                jdbc:postgresql://eat-db.cywcsttzyjee.us-east-1.rds.amazonaws.com:5432/eat?ssl=true&sslmode=verify-full&sslfactory=org.postgresql.ssl.SingleCertValidatingFactory&sslfactoryarg=classpath:ead/rds-ca-2019-us-east-1.pem""",
                        69543));

        assertEquals("""
                        jdbc:postgresql://eat-db.cywcsttzyjee.us-east-1.rds.amazonaws.com:69543/eat?ssl=true&sslmode=verify-full&sslfactory=org.postgresql.ssl.SingleCertValidatingFactory&sslfactoryarg=classpath:ead/rds-ca-2019-us-east-1.pem""",
                Io.changePortForUrl("""
                                jdbc:postgresql://eat-db.cywcsttzyjee.us-east-1.rds.amazonaws.com/eat?ssl=true&sslmode=verify-full&sslfactory=org.postgresql.ssl.SingleCertValidatingFactory&sslfactoryarg=classpath:ead/rds-ca-2019-us-east-1.pem""",
                        69543));

    }

    @Test
    @SneakyThrows
    void testURIs() {
        assertEquals("/a/b/c.jar", Io.getFileFromUri(new URI("jar:file:/a/b/c.jar!/_abc/props")));
        assertEquals("/a/b/c.jar", Io.getFileFromUri(new URI("jar:file:/a/b/c.jar")));
        assertEquals("/a/b/c.jar", Io.getFileFromUri(new URI("file:/a/b/c.jar")));
        assertEquals("/a/b/c.jar", Io.getFileFromUri(new URI("/a/b/c.jar")));

        assertTrue(Io.isJarUri(new URI("jar:file:/a/b/c.jar!/_abc/props")));
        assertTrue(Io.isJarUri(new URI("/a/b/c.jar!/_abc/props")));
        assertFalse(Io.isJarUri(new URI("/a/b/c.txt")));

        assertEquals("_abc/props", Io.getJarContextPathFromUri(new URI("jar:file:/a/b/c.jar!/_abc/props")));
        assertEquals("_abc/props", Io.getJarContextPathFromUri(new URI("/a/b/c.jar!/_abc/props")));
        assertEquals("", Io.getJarContextPathFromUri(new URI("jar:file:/a/b/c.jar")));
    }
}
