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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.jpountz.lz4.LZ4Factory;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.crypto.AESDecrypter;
import net.lingala.zip4j.crypto.AESEncrypter;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import sk.services.utils.LingalaZipHelper;
import sk.utils.functional.F1E;
import sk.utils.functional.O;
import sk.utils.statics.Ar;
import sk.utils.statics.Io;
import sk.utils.statics.Ma;
import sk.utils.statics.St;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static net.lingala.zip4j.model.enums.AesKeyStrength.*;
import static sk.utils.functional.O.empty;

@SuppressWarnings("unused")
@Slf4j
public class BytesImpl implements IBytes {

    @Override
    public <T> O<T> decodeCrcEncodedValue(String encodedValue, boolean isFromEnd, F1E<String, T> doWithValue) {
        for (int i = 11; i > 0; i--) {
            try {
                final String crc = encodedValue.substring(
                        isFromEnd ? encodedValue.length() - 1 - i : 0,
                        isFromEnd ? encodedValue.length() : i);
                if (Ma.isInt(crc)) {
                    long probableCrc = Ma.pl(crc);
                    String value = isFromEnd
                                   ? encodedValue.substring(0, encodedValue.length() - crc.length())
                                   : encodedValue.substring(i);
                    long actualCrc = crc32(St.utf8(value));
                    if (probableCrc == actualCrc) {
                        try {
                            T apply = doWithValue.apply(value);
                            return O.of(apply);
                        } catch (Exception e) {
                            log.error("", e);
                            throw e;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return O.empty();
    }

    @Override
    public byte[] encryptAes(String password, byte[] data, AesType type) {
        return encryptAes(password, data, switch (type) {
            case A_128 -> KEY_STRENGTH_128;
            case A_256 -> KEY_STRENGTH_256;
        });
    }

    @Override
    public byte[] decryptAes(String password, byte[] data) {
        return AesFormat.decrypt(password, data).getDecryptedMessage();
    }

    @Override
    public byte[] compressData(byte[] data) {
        val compressor = getLz4Factory().fastCompressor();
        val maxCompressedLength = compressor.maxCompressedLength(data.length);
        val bb = ByteBuffer.wrap(new byte[maxCompressedLength + 4]).putInt(0, data.length);
        val compressLength = compressor.compress(data, 0, data.length, bb.array(), 4, maxCompressedLength);
        return Arrays.copyOf(bb.array(), compressLength + 4);
    }

    @Override
    public byte[] unCompressData(byte[] data) {
        val sizeOfSource = ByteBuffer.wrap(data).getInt(0);
        val toRet = new byte[sizeOfSource];
        getLz4Factory().fastDecompressor().decompress(data, 4, toRet, 0, sizeOfSource);
        return toRet;
    }

    @Override
    @SneakyThrows
    public void zipFileOrFolderTo(File sourceFileOrFolder, File targetFile) {
        targetFile.mkdirs();
        Io.deleteIfExists(targetFile.getAbsolutePath());
        try (ZipFile zipFile = new ZipFile(targetFile)) {
            if (sourceFileOrFolder.isFile()) {
                zipFile.addFile(sourceFileOrFolder);
            } else {
                zipFile.addFolder(sourceFileOrFolder);
            }
        }
    }

    @Override
    public O<Map<String, byte[]>> unZipArchive(InputStream is, String password) {
        try {
            return O.of(LingalaZipHelper.unZipFromInput(is, O.of(new LingalaZipHelper.EncryptionParams(
                    password, EncryptionMethod.AES, KEY_STRENGTH_256
            ))));
        } catch (Exception e) {
            e.printStackTrace();
            return empty();
        }
    }

    @Override
    public O<Map<String, byte[]>> unZipArchive(InputStream is) {
        try {
            return O.of(LingalaZipHelper.unZipFromInput(is, empty()));
        } catch (Exception e) {
            e.printStackTrace();
            return empty();
        }
    }

    @Override
    public boolean zipArchive(OutputStream output, Map<String, byte[]> in) {
        try {
            LingalaZipHelper.zipToOutput(output, in, empty());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean zipArchiveWithPassword(OutputStream output, Map<String, byte[]> in, String password) {
        try {
            LingalaZipHelper.zipToOutput(output, in, O.of(new LingalaZipHelper.EncryptionParams(
                    password, EncryptionMethod.AES, KEY_STRENGTH_256
            )));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private LZ4Factory getLz4Factory() {
        return LZ4Factory.fastestInstance();
    }

    private byte[] encryptAes(String password, byte[] data, AesKeyStrength strength) {
        return AesFormat.encrypt(password, data, strength).getFullMessage();
    }

    @AllArgsConstructor
    private static class AesFormat {
        private static final byte[] MARKING_BYTES =
                new byte[]{-50, 50, 0, -50, 0, 1};

        @Getter byte[] decryptedMessage;
        byte[] encryptedMessage;
        byte[] saltBytes;
        byte[] passwordVerifier;
        @Getter byte[] fullMessage;

        @SneakyThrows
        public static AesFormat encrypt(String password, byte[] data, AesKeyStrength strength) {
            AESEncrypter aes = new AESEncrypter(password.toCharArray(), strength, true);
            byte[] cloned = Arrays.copyOf(data, data.length);
            int encryptedLength = aes.encryptData(cloned);
            byte[] encrypted = Arrays.copyOf(cloned, encryptedLength);
            byte[] salt = aes.getSaltBytes();
            byte[] verifier = aes.getDerivedPasswordVerifier();

            int aesTypeIndex = MARKING_BYTES.length;
            int saltLengthIndex = aesTypeIndex + 1;
            int saltIndex = saltLengthIndex + 1;
            int verifierLengthIndex = saltIndex + salt.length;
            int verifierIndex = verifierLengthIndex + 1;
            int messageLengthIndex = verifierIndex + verifier.length;
            int messageIndex = messageLengthIndex + 4;
            ByteBuffer bb = ByteBuffer
                    .allocate(messageIndex + encryptedLength)
                    .put(0, MARKING_BYTES)
                    .put(aesTypeIndex, (byte) switch (strength) {
                        case KEY_STRENGTH_128 -> 0;
                        case KEY_STRENGTH_192 -> 1;
                        case KEY_STRENGTH_256 -> 2;
                    })
                    .put(saltLengthIndex, (byte) salt.length)
                    .put(saltIndex, salt)
                    .put(verifierLengthIndex, (byte) verifier.length)
                    .put(verifierIndex, verifier)
                    .put(messageLengthIndex, Ar.intToByteArray(encrypted.length))
                    .put(messageIndex, encrypted);
            return new AesFormat(
                    data,
                    encrypted,
                    salt,
                    verifier,
                    bb.array()
            );
        }

        @SneakyThrows
        public static AesFormat decrypt(String password, byte[] data) {
            byte[] markingCheck = Arrays.copyOf(data, MARKING_BYTES.length);
            if (!Arrays.equals(markingCheck, MARKING_BYTES)) {
                throw new RuntimeException("Data is not Jsk AES encoded!");
            }

            int aesTypeIndex = MARKING_BYTES.length;
            int aesType = data[aesTypeIndex];
            AESExtraDataRecord record = new AESExtraDataRecord() {
                @Override
                public AesKeyStrength getAesKeyStrength() {
                    return switch (aesType) {
                        case 0 -> KEY_STRENGTH_128;
                        case 1 -> KEY_STRENGTH_192;
                        case 2 -> KEY_STRENGTH_256;
                        default -> throw new RuntimeException("Unknown aes type:" + aesType);
                    };
                }
            };

            int saltLengthIndex = aesTypeIndex + 1;
            int saltIndex = saltLengthIndex + 1;
            int saltLength = data[saltLengthIndex];
            byte[] salt = Ar.copy(data, saltIndex, saltLength);

            int verifierLengthIndex = saltIndex + salt.length;
            int verifierLength = data[verifierLengthIndex];
            int verifierIndex = verifierLengthIndex + 1;
            byte[] verifier = Ar.copy(data, verifierIndex, verifierLength);

            int messageLengthIndex = verifierIndex + verifier.length;
            int messageLength = Ar.bArrToInt(Ar.copy(data, messageLengthIndex, 4));
            int messageIndex = messageLengthIndex + 4;
            byte[] encryptedMsg = Ar.copy(data, messageIndex, messageLength);

            AESDecrypter aed = new AESDecrypter(record,
                    password.toCharArray(),
                    salt,
                    verifier,
                    true);
            byte[] decryptedMessage = Arrays.copyOf(encryptedMsg, encryptedMsg.length);
            int finalLength = aed.decryptData(decryptedMessage, 0, encryptedMsg.length);

            return new AesFormat(
                    Ar.copy(decryptedMessage, 0, finalLength),
                    encryptedMsg,
                    salt,
                    verifier,
                    data
            );
        }
    }
}
