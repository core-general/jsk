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
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import sk.services.bytes.IBytes;
import sk.services.rand.IRand;
import sk.utils.statics.St;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("unused")
@NoArgsConstructor
public class IdsImpl implements IIds {
    @Inject IRand random;
    @Inject IBytes bytes;
    JskHaikunator.LongAndShortHaikunator haikunator;

    public IdsImpl(IRand random, IBytes bytes) {
        this.random = random;
        this.bytes = bytes;
    }

    @PostConstruct
    public IdsImpl init() {
        haikunator = JskHaikunator.defaultHaikunators(random);
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
    public String longHaiku() {
        return haikunator.lng().haikunate();
    }


    @Override
    public String shortHaiku() {
        return haikunator.shrt().haikunate();
    }

    @NotNull
    private String enc64LeaveLettersAndNumbers(byte[] array) {
        final String s = bytes.enc64(array);
        return St.ss(s, 0, s.indexOf('=')).replaceAll("[+/=]", "0");
    }
}
