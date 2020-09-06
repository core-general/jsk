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

import lombok.val;
import net.jpountz.lz4.LZ4Factory;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import sk.services.utils.LingalaZipHelper;
import sk.utils.functional.O;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static sk.utils.functional.O.empty;

@SuppressWarnings("unused")
public class BytesImpl implements IBytes {
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
    public O<Map<String, byte[]>> unZipArchive(InputStream is, String password) {
        try {
            return O.of(LingalaZipHelper.unZipFromInput(is, O.of(new LingalaZipHelper.EncryptionParams(
                    password, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256
            ))));
        } catch (Exception e) {
            e.printStackTrace();
            return empty();
        }
    }

    @Override
    public O<Map<String, byte[]>> unZipArchive(InputStream is) {
        try {
            return O.of(LingalaZipHelper.unZipFromInput(is, O.empty()));
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
                    password, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256
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
}
