package sk.services.bytes;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import com.sangupta.bloomfilter.AbstractBloomFilter;
import com.sangupta.bloomfilter.BloomFilter;
import com.sangupta.bloomfilter.core.BitArray;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

public class BloomFilterImpl extends ICountSetExistence {
    BloomFilter<?> bf;
    SerializableJavaBitSetArray bits;

    public BloomFilterImpl(int maxItems, double probabilityOfFalsePositives) {
        bf = new AbstractBloomFilter<>(maxItems, probabilityOfFalsePositives) {
            @Override
            protected BitArray createBitArray(int numBits) {
                return bits = new SerializableJavaBitSetArray(numBits, maxItems, probabilityOfFalsePositives);
            }
        };
    }

    public BloomFilterImpl(byte[] serialized) {
        super(serialized);
    }

    @Override
    public boolean addElement(byte[] element) {
        return bf.add(element);
    }

    @Override
    public boolean isElementExist(byte[] element) {
        return bf.contains(element);
    }

    @Override
    public byte[] serialize() {
        return bits.serialize();
    }

    @Override
    protected void deSerialize(byte[] data) {
        bits = new SerializableJavaBitSetArray(data);

        bf = new AbstractBloomFilter<>(bits.getMaxItems(), bits.getProbability()) {
            @Override
            protected BitArray createBitArray(int numBits) {
                return bits;
            }
        };
    }

    //public static void main(String[] args) {
    //    final int maxItems = 100;
    //    BloomFilterImpl bf1 = new BloomFilterImpl(maxItems, 0.001);
    //    for (int i = 0; i < 100; i = i + 2) {
    //        bf1.addElement(i);
    //    }
    //    final byte[] serialize1 = bf1.serialize();
    //
    //    BloomFilterImpl bf2 = new BloomFilterImpl(serialize1);
    //    final byte[] serialize2 = bf2.serialize();
    //
    //    final int[] booleans1 = IntStream.range(0, maxItems)
    //            .map($ -> bf1.isElementExist($) ? 1 : 0)
    //            .toArray();
    //    final int[] booleans2 = IntStream.range(0, maxItems)
    //            .map($ -> bf2.isElementExist($) ? 1 : 0)
    //            .toArray();
    //
    //    final boolean b1 = bf2.addElement("12345");
    //    final boolean b2 = bf2.addElement("12345");
    //
    //    int i = 0;
    //}


    private class SerializableJavaBitSetArray implements BitArray {
        private static final int META_BYTES = 4/*maxItems*/ + 8/*probability*/ + 4/*size*/;
        @Getter final BitSet bitSet;
        final int size;

        @Getter private final int maxItems;
        @Getter private final double probability;

        public SerializableJavaBitSetArray(int numBits, int maxItems, double probability) {
            this.bitSet = new BitSet(numBits);
            this.size = this.bitSet.size();
            this.maxItems = maxItems;
            this.probability = probability;
        }

        public SerializableJavaBitSetArray(byte[] serialized) {
            final ByteBuffer wrap = ByteBuffer.wrap(serialized);

            this.maxItems = wrap.getInt();
            this.probability = wrap.getDouble();
            this.size = wrap.getInt();

            final byte[] allBytes = wrap.array();
            this.bitSet = BitSet.valueOf(Arrays.copyOfRange(allBytes, META_BYTES, allBytes.length));
        }

        public byte[] serialize() {
            final byte[] bytes = bitSet.toByteArray();
            return ByteBuffer.allocate(META_BYTES + bytes.length)
                    .putInt(maxItems)
                    .putDouble(probability)
                    .putInt(size)
                    .put(bytes)
                    .array();
        }

        @Override
        public void clear() {
            this.bitSet.clear();
        }

        @Override
        public boolean getBit(int index) {
            return this.bitSet.get(index);
        }

        @Override
        public boolean setBit(int index) {
            final boolean was = bitSet.get(index);
            bitSet.set(index);
            return !was;
        }

        @Override
        public void clearBit(int index) {
            this.bitSet.clear(index);
        }

        @Override
        public boolean setBitIfUnset(int index) {
            if (!this.bitSet.get(index)) {
                return this.setBit(index);
            }

            return false;
        }

        @Override
        public void or(BitArray bitArray) {
            if (bitArray == null) {
                throw new IllegalArgumentException("BitArray to OR with cannot be null");
            }

            if (this.size != bitArray.bitSize()) {
                throw new IllegalArgumentException("BitArray to OR with is of different length");
            }

            throw new RuntimeException("Operation not yet supported");
        }

        @Override
        public void and(BitArray bitArray) {
            if (bitArray == null) {
                throw new IllegalArgumentException("BitArray to OR with cannot be null");
            }

            if (this.size != bitArray.bitSize()) {
                throw new IllegalArgumentException("BitArray to OR with is of different length");
            }

            throw new RuntimeException("Operation not yet supported");
        }

        @Override
        public int bitSize() {
            return this.size;
        }

        @Override
        public void close() throws IOException {
            // do nothing
        }

    }

}
