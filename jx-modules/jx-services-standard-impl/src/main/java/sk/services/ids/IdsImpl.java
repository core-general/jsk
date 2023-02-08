package sk.services.ids;

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

import com.fasterxml.uuid.Generators;
import org.jetbrains.annotations.NotNull;
import sk.services.bytes.IBytes;
import sk.services.rand.SecureRandImpl;
import sk.services.time.ITime;
import sk.utils.statics.Im;
import sk.utils.statics.St;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static java.lang.Integer.parseInt;
import static sk.services.ids.JskHaikunator.LongAndShortHaikunator;

@SuppressWarnings("unused")
public class IdsImpl implements IIds {
    private SecureRandImpl random;
    @Inject IBytes bytes;
    @Inject ITime times;
    LongAndShortHaikunator haikunator;

    public IdsImpl(SecureRandImpl random, IBytes bytes) {
        this.random = random;
        this.bytes = bytes;
        init();
    }

    public IdsImpl(SecureRandImpl random) {
        this.random = random;
    }

    @PostConstruct
    public IdsImpl init() {
        haikunator = JskHaikunator.defaultHaikunators(random, times);
        return this;
    }

    @Override
    public UUID shortId() {
        return Generators.randomBasedGenerator(random.getRandom()).generate();
    }

    @Override
    public String customId(int length) {
        final Random random = this.random.getRandom();
        byte[] bytes = new byte[length];

        random.nextBytes(bytes);

        String s = enc64LeaveLettersAndNumbers(bytes);

        return s.substring(0, length);
    }

    @Override
    public UUID text2Uuid(String val) {
        return UUID.nameUUIDFromBytes(val.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] genUniquePngImageById(String id, int blockCount, int blockSize, Color bgColor) {
        final String hash = St.bytesToHex(this.bytes.md5(id.getBytes(StandardCharsets.UTF_8)));
        return createRandomPngImage(hash, blockCount, blockSize, bgColor);
    }

    @Override
    public byte[] genUniquePngImage(int blockCount, int blockSize, Color bgColor) {
        return createRandomPngImage(random.rndString(6, St.hex), blockCount, blockSize, bgColor);
    }

    @Override
    public String unique(byte[] val, int iterations, boolean valIsCloned) {
        if (iterations > 255) {
            throw new RuntimeException("Must be less than 255 bytes");
        }
        if (valIsCloned) {
            val = Arrays.copyOf(val, val.length);
        }
        ByteBuffer bb = ByteBuffer.allocate(iterations * 4);

        bb.putInt(0, this.bytes.crc32Signed(val));

        for (byte i = -128; i < 127 - (255 - iterations + 1); i++) {
            val[0] = i;
            bb.putInt((i + 128 + 1) * 4, this.bytes.crc32Signed(val));
        }

        return enc64LeaveLettersAndNumbers(bb.array());
    }

    @Override
    public LongAndShortHaikunator defaultHaikunators() {
        return haikunator;
    }

    public String tinyHaiku() {
        return haikunator.tiny().haikunate();
    }

    @Override
    public String longHaiku() {
        return haikunator.lng().haikunate();
    }

    @Override
    public String shortHaiku() {
        return haikunator.shrt().haikunate();
    }

    @Override
    public String timedHaiku() {
        return haikunator.timed().haikunate();
    }

    @NotNull
    private String enc64LeaveLettersAndNumbers(byte[] array) {
        final String s = bytes.enc64(array);
        return St.ss(s, 0, s.indexOf('=')).replaceAll("[+/=]", "0");
    }


    /**
     * 2^((blockCount÷2+1)×blockCount) × (256^3) combinations.
     * For default value 7 = 2^(7/3+1)*7*256^3 = 2^28 * 256^3=2^28*2^8*3=2^(28+24)=2^54 combinations
     */
    private byte[] createRandomPngImage(String hash, int blockCount, int blockSize, Color bgColor) {
        Random rnd = new Random(bytes.crc32(hash));
        hash = hash.substring(hash.length() - 6);
        Color clr = new Color(color(hash, 0), color(hash, 2), color(hash, 4));


        blockCount = blockCount / 2 + 1;
        int imgSize = blockSize * (blockCount * 2 - 1);

        BufferedImage bi = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = null;
        try {
            g2 = (Graphics2D) bi.getGraphics();
            g2.setColor(bgColor);
            g2.fillRect(0, 0, imgSize, imgSize);

            g2.setColor(clr);
            int mult = imgSize / ((blockCount * 2) - 1);
            for (int x = 0; x < blockCount; x++) {
                for (int y = 0; y < 2 * blockCount; y++) {
                    if (rnd.nextBoolean()) {
                        g2.fillRect(x * mult, y * mult, mult, mult);
                        g2.fillRect(imgSize - (x + 1) * mult, y * mult, mult, mult);
                    }
                }
            }

            return Im.savePngToBytes(bi);
        } finally {
            g2.dispose();
        }
    }

    private static int color(String hash, int i) {
        return parseInt(hash.substring(i, i + 2), 16);
    }
}
