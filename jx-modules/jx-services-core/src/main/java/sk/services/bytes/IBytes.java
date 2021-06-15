package sk.services.bytes;

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

import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Ma;
import sk.utils.statics.St;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("unused")
public interface IBytes {
    //region basic algorithms
    default byte[] md5(byte[] bytes) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            return Ex.thRow(e);
        }
    }

    default byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            return Ex.thRow(e);
        }
    }

    default long crc32(byte[] bytes) {
        Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        return checksum.getValue();
    }

    default long crc32(String utf) {
        return crc32(utf.getBytes(UTF_8));
    }

    default O<String> decodeCrcEncodedValue(String encodedValue) {
        for (int i = 11; i > 0; i--) {
            try {
                final String val = encodedValue.substring(0, i);
                if (Ma.isInt(val)) {
                    long probableCrc = Ma.pl(val);
                    String value = encodedValue.substring(i);
                    long actualCrc = crc32(St.utf8(value));
                    if (probableCrc == actualCrc) {
                        return O.of(value);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return O.empty();
    }

    default String enc64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    default String enc64Url(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    default byte[] dec64(String bytes) {
        return Base64.getDecoder().decode(bytes);
    }

    default byte[] dec64Url(String bytes) {
        return Base64.getUrlDecoder().decode(bytes);
    }

    default String urlEncode(String value) {
        return URLEncoder.encode(value, UTF_8);
    }

    default String urlDecode(String value) {
        return URLDecoder.decode(value, UTF_8);
    }

    //endregion

    //region compression
    byte[] compressData(byte[] data);

    byte[] unCompressData(byte[] data);

    default byte[] compressString(String input) {return compressData(input.getBytes(UTF_8));}

    default String unCompressString(byte[] input) {return new String(unCompressData(input), UTF_8);}
    //endregion

    //region zip archive
    String ZIP_KEY = "X";

    default O<byte[]> zipData(byte[] data) {
        return zipArchive(Cc.m(ZIP_KEY, data));
    }

    default O<byte[]> unZipData(byte[] zipped) {
        return unZipArchive(zipped).map($ -> $.get(ZIP_KEY));
    }

    default O<byte[]> zipString(String str) {
        return zipArchive(Cc.m(ZIP_KEY, str.getBytes(UTF_8)));
    }

    default O<String> unZipString(byte[] zipped) {
        return unZipArchive(zipped).map($ -> new String($.get(ZIP_KEY), UTF_8));
    }

    default O<byte[]> zipData(byte[] data, String password) {
        return zipArchive(Cc.m(ZIP_KEY, data), password);
    }

    default O<byte[]> unZipData(byte[] zipped, String password) {
        return unZipArchive(zipped, password).map($ -> $.get(ZIP_KEY));
    }

    default O<Map<String, byte[]>> unZipArchive(byte[] archive) {
        return unZipArchive(new ByteArrayInputStream(archive));
    }

    default O<Map<String, byte[]>> unZipArchive(byte[] archive, String password) {
        return unZipArchive(new ByteArrayInputStream(archive), password);
    }

    O<Map<String, byte[]>> unZipArchive(InputStream is);

    O<Map<String, byte[]>> unZipArchive(InputStream is, String password);

    default O<byte[]> zipArchive(Map<String, byte[]> in) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            boolean b = zipArchive(stream, in);
            return b ? O.of(stream.toByteArray()) : O.empty();
        } catch (IOException e) {
            e.printStackTrace();
            return O.empty();
        }
    }

    default O<byte[]> zipArchive(Map<String, byte[]> in, String password) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            boolean b = zipArchiveWithPassword(stream, in, password);
            return b ? O.of(stream.toByteArray()) : O.empty();
        } catch (IOException e) {
            e.printStackTrace();
            return O.empty();
        }
    }

    boolean zipArchive(OutputStream output, Map<String, byte[]> in);

    boolean zipArchiveWithPassword(OutputStream output, Map<String, byte[]> in, String password);
    //endregion
}
