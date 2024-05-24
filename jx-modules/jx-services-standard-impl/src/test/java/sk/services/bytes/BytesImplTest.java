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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import sk.services.http.CrcAndSize;
import sk.services.rand.RandImpl;
import sk.test.JskMockitoTest;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Io;
import sk.utils.statics.St;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BytesImplTest extends JskMockitoTest {
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
    void aes() {
        List<String> passwords = Cc.l("abc", "lkahjket5ijhh9uNY$*&Y$*@&y4826n4t8n&NY@N&*#$Y@*T8", "H%IUHG*G@&G$(@&$G<AJ31L:");
        List<String> messages = Cc.l("abc", St.repeat("a", 1000) + St.repeat("b", 1000) + St.repeat("c", 1000), """
                c40640d4-ef22-4d03-906a-a70c504bab8c,0017271b8b229d7ff41222d45b008aa2
                0b02e16b-5ff1-4b7a-8435-c881fbb9787b,001a7c6bbc489ad38a8928c129a9a243
                65aa06f1-9ab4-41e4-b028-3115888be5b7,001ae1ffd67e959e2115ed919545a3a7
                cb23c157-d402-47b3-a152-4e0b404ed1eb,0035dcfc028d15d20ced46b40e44fb63
                afeb4332-cbcb-4712-8bbc-bf41e4754b29,0036898e637dec2dc3c74d98b424b8e6
                f0c5b130-b6f1-457f-95e8-2afec00bd61d,0045236522b3c803ac298f567fe6d4ba
                dc1aec68-910b-492c-b93c-822f7607b913,0048c1ac046bec46bdf45873c308a7de
                383f3876-2bba-4ed9-bdb0-453ee85acf9a,0056cd5208d8b231199f0098d47b891b
                c50ded93-a4d2-4cf5-9184-2a99c6df707a,005d5b98ffdf2ded4031e6c6b3868eac
                1320e7db-52f5-4c59-b3b8-1913c891dcbd,005ecf64aea4e6c4920d5a628fe6b9e6
                01a9d026-dcc5-4a93-8f8a-ee90df4ead70,00600b4fc815cda30d824b889197b0df
                a04d49bc-2f62-4dba-956f-1f5e652c1bfc,006078ed989343155ce0242af7518875
                37fa558c-bc9f-4072-92ad-5ccbff0e844b,0060dd5434c713851b942666b2ae2076
                """);
        for (AesType aesType : AesType.values()) {
            Cc.eachWithEach(passwords, messages, (p, m) -> {
                byte[] data = m.getBytes(StandardCharsets.UTF_8);
                byte[] encrypted = bytes.encryptAes(p, data, aesType);
                assertNotEquals(data, encrypted);
                String decrypted = new String(bytes.decryptAes(p, encrypted), StandardCharsets.UTF_8);
                assertEquals(decrypted, m);
            });
        }
    }

    @Test
    void getResourceFolderRecursivelyFolderTest() {
        //also takes data from jx-util resource folder, it should be configured in maven with additionalClasspathElement
        Map<String, byte[]> resourceFolderRecursively = bytes.getResourceFolderRecursively("jx-test");
        Assertions.assertEquals("""
                jx-test/jx-test-1.txt -> 1
                jx-test/jx-test-2.txt -> 2
                jx-test/jx-test-2/jx-test-21.txt -> 21
                jx-test/jx-test-2/jx-test-22.txt -> 22
                jx-test/jx-test-2/jx-test-3/jx-test-231.txt -> 231
                jx-test/jx-test-2/jx-test-3/jx-test-232.txt -> 232
                jx-test/jx-test-2/jx-test-3/non-jx-test-233.txt -> 233
                jx-test/jx-test-2/non-jx-test-23.txt -> 23
                jx-test/jx-test-3.txt -> 3
                jx-test/jx-test-4.txt -> 4
                jx-test/jx-test/jx-test-1.txt -> 11
                jx-test/jx-test/jx-test-2.txt -> 12
                jx-test/jx-test/jx-test-3.txt -> 33
                jx-test/jx-test/jx-test-4.txt -> 44
                jx-test/non-jx-test-3.txt -> 3
                jx-test/non-jx-test/jx-test-1.txt -> 11
                jx-test/non-jx-test/jx-test-2.txt -> 22""", Cc.join("\n", resourceFolderRecursively.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map($ -> $.getKey() + " -> " + new String($.getValue()))));
    }

    @Test
    public void getResourceFolderRecursivelyJarTest() {
        URI uri = Io.getResourceUri("jx-test-additional/jx-test.jar").get();
        String fileName = Io.getFileFromUri(uri);
        Map<String, byte[]> resourceFolderRecursively =
                bytes.getResourceFolderRecursively("jx-test",
                        w -> Cc.l(URI.create("jar:file:" + fileName + "!" + St.startWith(w, "/")).toURL()));

        Assertions.assertEquals("""
                jx-test-1.txt -> 1
                jx-test-2.txt -> 2
                jx-test-2/jx-test-21.txt -> 21
                jx-test-2/jx-test-22.txt -> 22
                jx-test-2/jx-test-3/jx-test-231.txt -> 231
                jx-test-2/jx-test-3/jx-test-232.txt -> 232
                jx-test-2/jx-test-3/non-jx-test-233.txt -> 233
                jx-test-2/non-jx-test-23.txt -> 23
                jx-test/jx-test-1.txt -> 11
                jx-test/jx-test-2.txt -> 12""", Cc.join("\n", resourceFolderRecursively.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map($ -> $.getKey() + " -> " + new String($.getValue()))));
    }

    @Test
    public void decodeCrcEncodedValueTest() {
        {
            String s = bytes.decodeCrcEncodedValue("V1342839628", true, w -> w)
                    .orElseThrow(() -> new RuntimeException("Wrong: V1342839628"));
            assertEquals(s, "V", "Wrong: V1342839628");
            assertEquals(O.empty(), bytes.decodeCrcEncodedValue("V1342839628", true, w -> Ex.thRow()));
        }
        IntStream.range(0, 1_000).parallel().forEach($ -> {
            final String val = rnd.rndString(rnd.rndInt(1, 5), St.engENGDig);
            final long crc = bytes.crc32(val.getBytes(StandardCharsets.UTF_8));
            {
                String s = bytes.decodeCrcEncodedValue(crc + val, false, w -> w)
                        .orElseThrow(() -> new RuntimeException("Wrong: " + crc + val));
                assertEquals(s, val, "Wrong: " + crc + val);
            }
            {
                String s = bytes.decodeCrcEncodedValue(val + crc, true, w -> w)
                        .orElseThrow(() -> new RuntimeException("Wrong: " + val + crc));
                assertEquals(s, val, "Wrong: " + val + crc);
            }
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
