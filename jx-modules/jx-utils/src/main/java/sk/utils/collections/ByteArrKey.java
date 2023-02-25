package sk.utils.collections;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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
import sk.utils.javafixes.Base62;
import sk.utils.statics.Ar;
import sk.utils.statics.St;

import java.util.Arrays;
import java.util.UUID;

@AllArgsConstructor
public class ByteArrKey {
    private final byte[] bytes;

    public byte[] getBytes() {
        return bytes.clone();
    }

    public long asLong() {
        if (bytes.length == 8) {
            return Ar.bArrToLong(bytes);
        } else {
            throw new UnsupportedOperationException("Can't convert to long value of " + bytes.length + " bytes");
        }
    }

    public int asInt() {
        if (bytes.length == 4) {
            return Ar.bArrToInt(bytes);
        } else {
            throw new UnsupportedOperationException("Can't convert to int value of " + bytes.length + " bytes");
        }
    }

    public UUID asUUID() {
        if (bytes.length == 16) {
            return UUID.nameUUIDFromBytes(bytes);
        } else {
            throw new UnsupportedOperationException("Can't convert to UUID value of " + bytes.length + " bytes");
        }
    }

    public String asHex() {
        return St.bytesToHex(bytes);
    }

    public String asBase62() {
        return Base62.encode(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        ByteArrKey that = (ByteArrKey) o;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return Arrays.toString(bytes);
    }
}
