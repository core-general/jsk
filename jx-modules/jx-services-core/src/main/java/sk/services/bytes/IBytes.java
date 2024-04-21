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

import lombok.SneakyThrows;
import sk.services.http.CrcAndSize;
import sk.services.http.EtagAndSize;
import sk.utils.collections.ByteArrKey;
import sk.utils.functional.O;
import sk.utils.javafixes.Base62;
import sk.utils.statics.*;
import sk.utils.tree.Tree;
import sk.utils.tree.TreePath;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.zip.*;

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

    //region CRC
    @SneakyThrows
    default CrcAndSize crc32(InputStream inputStream, int bufferSize, Io.StreamPumpInterceptor also) {
        return crc32(inputStream, Io.NONE(), bufferSize, also);
    }

    @SneakyThrows
    default CrcAndSize crc32(InputStream is, OutputStream os, int bufferSize, Io.StreamPumpInterceptor also) {
        Checksum checksum = new CRC32();

        long fullLength = Io.streamPumpLength(
                is,
                os,
                bufferSize,
                (buffer, size) -> checksum.update(buffer, 0, size));

        return new CrcAndSize(checksum.getValue(), fullLength);
    }


    default EtagAndSize calcEtagAndSize(byte[] data) {
        return new EtagAndSize(St.bytesToHex(md5(data)), data.length);
    }

    default CrcAndSize calcCrcAndSize(byte[] data) {
        return calcCrcAndSize(new ByteArrayInputStream(data), 64 * 1024);
    }

    default CrcAndSize calcCrcAndSize(InputStream is, int buffer) {
        return crc32(is, Io.NONE(), buffer, Io.NONE);
    }

    @SneakyThrows
    default CrcAndSize calcCrcAndSize(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return crc32(fis, Io.NONE(), 16 * 1024, Io.NONE);
        }
    }

    default long crc32(byte[] bytes) {
        return crc32(bytes, 0, bytes.length);
    }

    default long crc32(byte[] bytes, int offset, int size) {
        Checksum checksum = new CRC32();
        checksum.update(bytes, offset, size);
        return checksum.getValue();
    }

    default long crc32c(byte[] bytes) {
        Checksum checksum = new CRC32C();
        checksum.update(bytes, 0, bytes.length);
        return checksum.getValue();
    }

    default int crc32Signed(byte[] bytes) {
        return crc32Signed(bytes, 0, bytes.length);
    }

    default int crc32Signed(byte[] bytes, int offset, int limit) {
        final long l = crc32(bytes, offset, limit);
        return (int) (l - Integer.MAX_VALUE / 2);
    }

    default long crc32(String utf) {
        return crc32(utf.getBytes(UTF_8));
    }

    default ByteArrKey crcAny64(byte[] toEncode) {
        return crcAny(toEncode, 8);
    }

    default ByteArrKey crcAny128(byte[] toEncode) {
        return crcAny(toEncode, 16);
    }

    default ByteArrKey crcAny256(byte[] toEncode) {
        return crcAny(toEncode, 32);
    }

    default ByteArrKey crcAny(byte[] toEncode, int byteCount) {
        int numOfSlices = byteCount / 4 + (byteCount % 4 == 0 ? 0 : 1);
        int layerSizeBytes = numOfSlices * 4;
        int sliceSize = toEncode.length / numOfSlices + (toEncode.length % numOfSlices == 0 ? 0 : 1);
        int shuffler = crc32Signed(String.valueOf(crc32Signed(toEncode) << 1).getBytes());
        ByteBuffer bb = ByteBuffer.allocate(numOfSlices * 4);
        for (int i = 0; i < numOfSlices; i++) {
            int offset = i * sliceSize;
            offset = offset >= toEncode.length ? toEncode.length - 1 : offset;
            int curSlizeSize = Math.min(sliceSize, toEncode.length - offset);
            int i1 = crc32Signed(toEncode, offset, curSlizeSize);
            int value = crc32Signed(String.valueOf(i1 ^ shuffler + i).getBytes());
            bb.putInt(i * 4, value);
        }

        return new ByteArrKey(Arrays.copyOf(bb.array(), byteCount));
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
    //endregion


    //region Base64/62
    default String enc64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    default String enc62(byte[] bytes) {
        return Base62.encode(bytes);
    }

    default String enc64Url(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    default byte[] dec64(String bytes) {
        return Base64.getDecoder().decode(bytes);
    }

    default byte[] dec62(String bytes) {
        return Base62.decode(bytes);
    }

    default byte[] dec64Url(String bytes) {
        return Base64.getUrlDecoder().decode(bytes);
    }
    //endregion


    //region UrlEncode
    default String urlEncode(String value) {
        return URLEncoder.encode(value, UTF_8);
    }

    default String urlDecode(String value) {
        return URLDecoder.decode(value, UTF_8);
    }
    //endregion


    //region BCrypt
    default String bcryptHash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * @return true - ok, false - not ok
     */
    default boolean bcryptCheck(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
    //endregion


    //endregion

    //region compression
    byte[] compressData(byte[] data);

    byte[] unCompressData(byte[] data);

    default byte[] compressString(String input) {return compressData(input.getBytes(UTF_8));}

    default String unCompressString(byte[] input) {return new String(unCompressData(input), UTF_8);}
    //endregion

    //region zip archive
    String ZIP_KEY = "X";


    void zipFileOrFolderTo(File sourceFileOrFolder, File targetFile);

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
        if (archive.length == 0) {
            return O.empty();
        }
        return unZipArchive(new ByteArrayInputStream(archive));
    }

    default O<Map<String, byte[]>> unZipArchive(byte[] archive, String password) {
        if (archive.length == 0) {
            return O.empty();
        }
        return unZipArchive(new ByteArrayInputStream(archive), password);
    }

    default O<Tree<String, byte[]>> unZipArchiveTree(byte[] archive) {
        return unzipTreeInternal(unZipArchive(archive));
    }

    default O<Tree<String, byte[]>> unZipArchiveTree(byte[] archive, String password) {
        return unzipTreeInternal(unZipArchive(archive, password));
    }

    private static O<Tree<String, byte[]>> unzipTreeInternal(O<Map<String, byte[]>> oRes) {
        return oRes.map(res -> {
            Tree<String, byte[]> tree = Tree.create();
            res.forEach((k, v) -> {
                tree.setVal(TreePath.path(k.split("/")), v);
            });
            return tree;
        });
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

    //region gzip direct

    default byte[] gzip(String data) {
        return gzip(data.getBytes(UTF_8));
    }

    default String unGzipString(byte[] data) {
        return new String(unGzipBytes(data), UTF_8);
    }

    default byte[] gzip(byte[] data) {
        if (data == null || data.length == 0) {
            throw new RuntimeException("zip data is bad");
        }

        try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             final GZIPOutputStream gzipStream = new GZIPOutputStream(bytes)) {
            gzipStream.write(data);
            gzipStream.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Can't compress");
        }
    }

    default byte[] unGzipBytes(byte[] data) {
        if (data == null || data.length == 0) {
            throw new RuntimeException("unGzip data is bad");
        }

        try (final GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(data));) {
            return Io.streamPump(gzipInput);
        } catch (IOException e) {
            throw new RuntimeException("Error while decompression!", e);
        }
    }
    //endregion
}
