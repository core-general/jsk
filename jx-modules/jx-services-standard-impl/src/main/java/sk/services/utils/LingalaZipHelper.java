package sk.services.utils;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import sk.utils.functional.O;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

public class LingalaZipHelper {
    @AllArgsConstructor
    @Data
    public static class EncryptionParams {
        String password;
        EncryptionMethod encryptionMethod;
        AesKeyStrength aesKeyStrength;
    }

    public static void zipToOutput(
            OutputStream output,
            Map<String, byte[]> filesToAdd,
            O<EncryptionParams> encrypt
    ) throws Exception {

        ZipParameters zipParameters = buildZipParameters(CompressionMethod.DEFLATE, encrypt.isPresent(),
                encrypt.map(EncryptionParams::getEncryptionMethod).orElse(null),
                encrypt.map(EncryptionParams::getAesKeyStrength).orElse(null));
        byte[] buff = new byte[4096];
        int readLen;

        try (ZipOutputStream zos =
                     initializeZipOutputStream(output, encrypt.isPresent(),
                             encrypt.map($ -> $.getPassword().toCharArray()).orElse(null))) {
            for (Map.Entry<String, byte[]> kv : filesToAdd.entrySet()) {
                zipParameters.setFileNameInZip(kv.getKey());
                zos.putNextEntry(zipParameters);

                try (InputStream inputStream = new ByteArrayInputStream(kv.getValue())) {
                    while ((readLen = inputStream.read(buff)) != -1) {
                        zos.write(buff, 0, readLen);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    public static Map<String, byte[]> unZipFromInput(
            InputStream output,
            O<EncryptionParams> encrypt
    ) throws IOException {
        byte[] buff = new byte[4096];
        int readLen;

        Map<String, byte[]> toRet = new TreeMap<>();
        try (ZipInputStream zos =
                     initializeZipInputStream(output, encrypt.isPresent(),
                             encrypt.map($ -> $.getPassword().toCharArray()).orElse(null))) {
            LocalFileHeader nextEntry;
            while ((nextEntry = zos.getNextEntry()) != null) {
                String fileName = nextEntry.getFileName();
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    while ((readLen = zos.read(buff)) != -1) {
                        outputStream.write(buff, 0, readLen);
                    }
                    if (!fileName.endsWith("/") && !fileName.endsWith("\\")) {
                        toRet.put(fileName, outputStream.toByteArray());
                    }
                }
            }
        }
        return toRet;
    }

    private static ZipOutputStream initializeZipOutputStream(OutputStream outputStream, boolean encrypt, char[] password)
            throws IOException {
        if (encrypt) {
            return new ZipOutputStream(outputStream, password);
        }
        return new ZipOutputStream(outputStream);
    }

    private static ZipInputStream initializeZipInputStream(InputStream outputStream, boolean encrypt, char[] password) {
        if (encrypt) {
            return new ZipInputStream(outputStream, password);
        }
        return new ZipInputStream(outputStream);
    }

    @SuppressWarnings("SameParameterValue")
    private static ZipParameters buildZipParameters(CompressionMethod compressionMethod, boolean encrypt,
            EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionMethod(compressionMethod);
        zipParameters.setEncryptionMethod(encryptionMethod);
        zipParameters.setAesKeyStrength(aesKeyStrength);
        zipParameters.setEncryptFiles(encrypt);
        return zipParameters;
    }
}
