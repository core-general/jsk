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
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import sk.services.bytes.IBytes;
import sk.services.rand.IRand;
import sk.utils.statics.St;

import javax.inject.Inject;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SuppressWarnings("unused")
@AllArgsConstructor
@NoArgsConstructor
public class IdsImpl implements IIds {
    @Inject IRand random;
    @Inject IBytes bytes;

    @Override
    public UUID shortId() {
        return Generators.randomBasedGenerator(random.getRandom()).generate();
    }

    @Override
    public String customId(int length) {
        String s = new BigInteger(length * 5, random.getRandom()).toString(32);
        if (s.length() < length) {
            s = St.repeat("0", length - s.length()) + s;
        }
        return s;
    }

    @Override
    @SneakyThrows
    public UUID uniqueFrom(String val) {
        byte[] l1 = bytes.sha256(val.getBytes(StandardCharsets.UTF_8));
        ByteBuffer wrap = ByteBuffer.wrap(l1);
        return new UUID(wrap.getLong(0), wrap.getLong(8));
    }
}
