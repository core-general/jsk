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

import java.nio.ByteBuffer;


public class CountMinSketchCounterImpl extends ICountElementsInGroups {
    CountMinSketch cms;

    public CountMinSketchCounterImpl(float epsOfTotalCount, float confidence) {
        cms = new CountMinSketch(epsOfTotalCount, confidence);
    }


    public CountMinSketchCounterImpl(float epsOfTotalCount, float confidence, long seed) {
        cms = new CountMinSketch(epsOfTotalCount, confidence);
    }

    public CountMinSketchCounterImpl(byte[] serialized) {
        super(serialized);
    }

    @Override
    public void addElement(byte[] element) {
        cms.set(element);
    }

    @Override
    public long getElementCount(byte[] element) {
        return cms.getEstimatedCount(element);
    }

    @Override
    public byte[] serialize() {
        return CountMinSketch.serialize(cms);
    }

    @Override
    protected void deSerialize(byte[] data) {
        cms = CountMinSketch.deserialize(data);
    }

    //public static void main(String[] args) {
    //    CountMinSketchCounterImpl impl = new CountMinSketchCounterImpl(0.2f, 0.05f);
    //    final int ELEMENT_NUM = 16;
    //    for (int i = 1; i <= ELEMENT_NUM; i++) {
    //        for (int j = 0; j < 5000000; j++) {
    //            impl.addElement(i);
    //        }
    //    }
    //
    //    List<X2<Integer, Long>> vals = Cc.l();
    //    for (int i = 1; i <= ELEMENT_NUM; i++) {
    //        vals.add(X.x(i, impl.getElementCount(i)));
    //    }
    //
    //    System.out.println(Cc.join("\n", vals));
    //    System.out.println(impl.serialize().length);
    //}

    /**
     * Licensed to the Apache Software Foundation (ASF) under one or more
     * contributor license agreements. See the NOTICE file distributed with this
     * work for additional information regarding copyright ownership. The ASF
     * licenses this file to you under the Apache License, Version 2.0 (the
     * "License"); you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
     * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
     * License for the specific language governing permissions and limitations under
     * the License.
     */
    /**
     * Count Min sketch is a probabilistic data structure for finding the frequency of events in a
     * stream of data. The data structure accepts two parameters epsilon and delta, epsilon specifies
     * the error in estimation and delta specifies the probability that the estimation is wrong (or the
     * confidence interval). The default values are 1% estimation error (epsilon) and 99% confidence
     * (1 - delta). Tuning these parameters results in increase or decrease in the size of the count
     * min sketch. The constructor also accepts width and depth parameters. The relationship between
     * width and epsilon (error) is width = Math.ceil(Math.exp(1.0)/epsilon). In simpler terms, the
     * lesser the error is, the greater is the width and hence the size of count min sketch.
     * The relationship between delta and depth is depth = Math.ceil(Math.log(1.0/delta)). In simpler
     * terms, the more the depth of the greater is the confidence.
     * The way it works is, if we need to estimate the number of times a certain key is inserted (or appeared in
     * the stream), count min sketch uses pairwise independent hash functions to map the key to
     * different locations in count min sketch and increment the counter.
     * <p/>
     * For example, if width = 10 and depth = 4, lets assume the hashcodes
     * for key "HELLO" using pairwise independent hash functions are 9812121, 6565512, 21312312, 8787008
     * respectively. Then the counter in hashcode % width locations are incremented.
     * <p/>
     * 0   1   2   3   4   5   6   7   8   9
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * <p/>
     * Now for a different key "WORLD", let the hashcodes be 23123123, 45354352, 8567453, 12312312.
     * As we can see below there is a collision for 2nd hashcode
     * <p/>
     * 0   1   2   3   4   5   6   7   8   9
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 1 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 0 | 1 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * --- --- --- --- --- --- --- --- --- ---
     * | 0 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
     * --- --- --- --- --- --- --- --- --- ---
     * <p/>
     * Now, to get the estimated count for key "HELLO", same process is repeated again to find the
     * values in each position and the estimated count will be the minimum of all values (to account for
     * hash collisions).
     * <p/>
     * estimatedCount("HELLO") = min(1, 2, 1, 1)
     * <p/>
     * so even if there are multiple hash collisions, the returned value will be the best estimate
     * (upper bound) for the given key. The actual count can never be greater than this value.
     */
    public static class CountMinSketch {
        // 1% estimation error with 1% probability (99% confidence) that the estimation breaks this limit
        private static final float DEFAULT_DELTA = 0.01f;
        private static final float DEFAULT_EPSILON = 0.01f;
        private final int w;
        private final int d;
        private final int[][] multiset;

        public CountMinSketch() {
            this(DEFAULT_DELTA, DEFAULT_EPSILON);
        }

        public CountMinSketch(float delta, float epsilon) {
            this.w = (int) Math.ceil(Math.exp(1.0) / epsilon);
            this.d = (int) Math.ceil(Math.log(1.0 / delta));
            this.multiset = new int[d][w];
        }

        public CountMinSketch(int width, int depth) {
            this.w = width;
            this.d = depth;
            this.multiset = new int[d][w];
        }

        private CountMinSketch(int width, int depth, int[][] ms) {
            this.w = width;
            this.d = depth;
            this.multiset = ms;
        }

        public int getWidth() {
            return w;
        }

        public int getDepth() {
            return d;
        }

        /**
         * Returns the size in bytes after serialization.
         *
         * @return serialized size in bytes
         */
        public long getSizeInBytes() {
            return ((w * d) + 2) * (Integer.SIZE / 8);
        }

        public void set(byte[] key) {
            // We use the trick mentioned in "Less Hashing, Same Performance: Building a Better Bloom Filter"
            // by Kirsch et.al. From abstract 'only two hash functions are necessary to effectively
            // implement a Bloom filter without any loss in the asymptotic false positive probability'
            // The paper also proves that the same technique (using just 2 pairwise independent hash functions)
            // can be used for Count-Min sketch.

            // Lets split up 64-bit hashcode into two 32-bit hashcodes and employ the technique mentioned
            // in the above paper
            long hash64 = Murmur3.hash64(key);
            int hash1 = (int) hash64;
            int hash2 = (int) (hash64 >>> 32);
            for (int i = 1; i <= d; i++) {
                int combinedHash = hash1 + (i * hash2);
                // hashcode should be positive, flip all the bits if it's negative
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                int pos = combinedHash % w;
                multiset[i - 1][pos] += 1;
            }
        }

        public void setString(String val) {
            set(val.getBytes());
        }

        public void setByte(byte val) {
            set(new byte[]{val});
        }

        public void setInt(int val) {
            // puts int in little endian order
            set(intToByteArrayLE(val));
        }


        public void setLong(long val) {
            // puts long in little endian order
            set(longToByteArrayLE(val));
        }

        public void setFloat(float val) {
            setInt(Float.floatToIntBits(val));
        }

        public void setDouble(double val) {
            setLong(Double.doubleToLongBits(val));
        }

        private static byte[] intToByteArrayLE(int val) {
            return new byte[]{(byte) (val >> 0),
                              (byte) (val >> 8),
                              (byte) (val >> 16),
                              (byte) (val >> 24)};
        }

        private static byte[] longToByteArrayLE(long val) {
            return new byte[]{(byte) (val >> 0),
                              (byte) (val >> 8),
                              (byte) (val >> 16),
                              (byte) (val >> 24),
                              (byte) (val >> 32),
                              (byte) (val >> 40),
                              (byte) (val >> 48),
                              (byte) (val >> 56),};
        }

        public int getEstimatedCount(byte[] key) {
            long hash64 = Murmur3.hash64(key);
            int hash1 = (int) hash64;
            int hash2 = (int) (hash64 >>> 32);
            int min = Integer.MAX_VALUE;
            for (int i = 1; i <= d; i++) {
                int combinedHash = hash1 + (i * hash2);
                // hashcode should be positive, flip all the bits if it's negative
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                int pos = combinedHash % w;
                min = Math.min(min, multiset[i - 1][pos]);
            }

            return min;
        }

        public int getEstimatedCountString(String val) {
            return getEstimatedCount(val.getBytes());
        }

        public int getEstimatedCountByte(byte val) {
            return getEstimatedCount(new byte[]{val});
        }

        public int getEstimatedCountInt(int val) {
            return getEstimatedCount(intToByteArrayLE(val));
        }

        public int getEstimatedCountLong(long val) {
            return getEstimatedCount(longToByteArrayLE(val));
        }

        public int getEstimatedCountFloat(float val) {
            return getEstimatedCountInt(Float.floatToIntBits(val));
        }

        public int getEstimatedCountDouble(double val) {
            return getEstimatedCountLong(Double.doubleToLongBits(val));
        }

        /**
         * Merge the give count min sketch with current one. Merge will throw RuntimeException if the
         * provided CountMinSketch is not compatible with current one.
         *
         * @param that - the one to be merged
         */
        public void merge(CountMinSketch that) {
            if (that == null) {
                return;
            }

            if (this.w != that.w) {
                throw new RuntimeException("Merge failed! Width of count min sketch do not match!" +
                        "this.width: " + this.getWidth() + " that.width: " + that.getWidth());
            }

            if (this.d != that.d) {
                throw new RuntimeException("Merge failed! Depth of count min sketch do not match!" +
                        "this.depth: " + this.getDepth() + " that.depth: " + that.getDepth());
            }

            for (int i = 0; i < d; i++) {
                for (int j = 0; j < w; j++) {
                    this.multiset[i][j] += that.multiset[i][j];
                }
            }
        }

        /**
         * Serialize the count min sketch to byte array. The format of serialization is width followed by
         * depth followed by integers in multiset from row1, row2 and so on..
         *
         * @return serialized byte array
         */
        public static byte[] serialize(CountMinSketch cms) {
            long serializedSize = cms.getSizeInBytes();
            ByteBuffer bb = ByteBuffer.allocate((int) serializedSize);
            bb.putInt(cms.getWidth());
            bb.putInt(cms.getDepth());
            for (int i = 0; i < cms.getDepth(); i++) {
                for (int j = 0; j < cms.getWidth(); j++) {
                    bb.putInt(cms.multiset[i][j]);
                }
            }
            bb.flip();
            return bb.array();
        }

        /**
         * Deserialize the serialized count min sketch.
         *
         * @param serialized - serialized count min sketch
         * @return deserialized count min sketch object
         */
        public static CountMinSketch deserialize(byte[] serialized) {
            ByteBuffer bb = ByteBuffer.allocate(serialized.length);
            bb.put(serialized);
            bb.flip();
            int width = bb.getInt();
            int depth = bb.getInt();
            int[][] multiset = new int[depth][width];
            for (int i = 0; i < depth; i++) {
                for (int j = 0; j < width; j++) {
                    multiset[i][j] = bb.getInt();
                }
            }
            CountMinSketch cms = new CountMinSketch(width, depth, multiset);
            return cms;
        }
    }

    public static class Murmur3 {
        // Constants for 32 bit variant
        private static final int C1_32 = 0xcc9e2d51;
        private static final int C2_32 = 0x1b873593;
        private static final int R1_32 = 15;
        private static final int R2_32 = 13;
        private static final int M_32 = 5;
        private static final int N_32 = 0xe6546b64;

        // Constants for 128 bit variant
        private static final long C1 = 0x87c37b91114253d5L;
        private static final long C2 = 0x4cf5ad432745937fL;
        private static final int R1 = 31;
        private static final int R2 = 27;
        private static final int R3 = 33;
        private static final int M = 5;
        private static final int N1 = 0x52dce729;
        private static final int N2 = 0x38495ab5;

        private static final int DEFAULT_SEED = 0;

        /**
         * Murmur3 32-bit variant.
         *
         * @param data - input byte array
         * @return - hashcode
         */
        public static int hash32(byte[] data) {
            return hash32(data, data.length, DEFAULT_SEED);
        }

        /**
         * Murmur3 32-bit variant.
         *
         * @param data   - input byte array
         * @param length - length of array
         * @param seed   - seed. (default 0)
         * @return - hashcode
         */
        public static int hash32(byte[] data, int length, int seed) {
            int hash = seed;
            final int nblocks = length >> 2;

            // body
            for (int i = 0; i < nblocks; i++) {
                int i_4 = i << 2;
                int k = (data[i_4] & 0xff)
                        | ((data[i_4 + 1] & 0xff) << 8)
                        | ((data[i_4 + 2] & 0xff) << 16)
                        | ((data[i_4 + 3] & 0xff) << 24);

                // mix functions
                k *= C1_32;
                k = Integer.rotateLeft(k, R1_32);
                k *= C2_32;
                hash ^= k;
                hash = Integer.rotateLeft(hash, R2_32) * M_32 + N_32;
            }

            // tail
            int idx = nblocks << 2;
            int k1 = 0;
            switch (length - idx) {
                case 3:
                    k1 ^= data[idx + 2] << 16;
                case 2:
                    k1 ^= data[idx + 1] << 8;
                case 1:
                    k1 ^= data[idx];

                    // mix functions
                    k1 *= C1_32;
                    k1 = Integer.rotateLeft(k1, R1_32);
                    k1 *= C2_32;
                    hash ^= k1;
            }

            // finalization
            hash ^= length;
            hash ^= (hash >>> 16);
            hash *= 0x85ebca6b;
            hash ^= (hash >>> 13);
            hash *= 0xc2b2ae35;
            hash ^= (hash >>> 16);

            return hash;
        }

        /**
         * Murmur3 64-bit variant. This is essentially MSB 8 bytes of Murmur3 128-bit variant.
         *
         * @param data - input byte array
         * @return - hashcode
         */
        public static long hash64(byte[] data) {
            return hash64(data, data.length, DEFAULT_SEED);
        }

        /**
         * Murmur3 64-bit variant. This is essentially MSB 8 bytes of Murmur3 128-bit variant.
         *
         * @param data   - input byte array
         * @param length - length of array
         * @param seed   - seed. (default is 0)
         * @return - hashcode
         */
        public static long hash64(byte[] data, int length, int seed) {
            long hash = seed;
            final int nblocks = length >> 3;

            // body
            for (int i = 0; i < nblocks; i++) {
                final int i8 = i << 3;
                long k = ((long) data[i8] & 0xff)
                        | (((long) data[i8 + 1] & 0xff) << 8)
                        | (((long) data[i8 + 2] & 0xff) << 16)
                        | (((long) data[i8 + 3] & 0xff) << 24)
                        | (((long) data[i8 + 4] & 0xff) << 32)
                        | (((long) data[i8 + 5] & 0xff) << 40)
                        | (((long) data[i8 + 6] & 0xff) << 48)
                        | (((long) data[i8 + 7] & 0xff) << 56);

                // mix functions
                k *= C1;
                k = Long.rotateLeft(k, R1);
                k *= C2;
                hash ^= k;
                hash = Long.rotateLeft(hash, R2) * M + N1;
            }

            // tail
            long k1 = 0;
            int tailStart = nblocks << 3;
            switch (length - tailStart) {
                case 7:
                    k1 ^= ((long) data[tailStart + 6] & 0xff) << 48;
                case 6:
                    k1 ^= ((long) data[tailStart + 5] & 0xff) << 40;
                case 5:
                    k1 ^= ((long) data[tailStart + 4] & 0xff) << 32;
                case 4:
                    k1 ^= ((long) data[tailStart + 3] & 0xff) << 24;
                case 3:
                    k1 ^= ((long) data[tailStart + 2] & 0xff) << 16;
                case 2:
                    k1 ^= ((long) data[tailStart + 1] & 0xff) << 8;
                case 1:
                    k1 ^= ((long) data[tailStart] & 0xff);
                    k1 *= C1;
                    k1 = Long.rotateLeft(k1, R1);
                    k1 *= C2;
                    hash ^= k1;
            }

            // finalization
            hash ^= length;
            hash = fmix64(hash);

            return hash;
        }

        /**
         * Murmur3 128-bit variant.
         *
         * @param data - input byte array
         * @return - hashcode (2 longs)
         */
        public static long[] hash128(byte[] data) {
            return hash128(data, data.length, DEFAULT_SEED);
        }

        /**
         * Murmur3 128-bit variant.
         *
         * @param data   - input byte array
         * @param length - length of array
         * @param seed   - seed. (default is 0)
         * @return - hashcode (2 longs)
         */
        public static long[] hash128(byte[] data, int length, int seed) {
            long h1 = seed;
            long h2 = seed;
            final int nblocks = length >> 4;

            // body
            for (int i = 0; i < nblocks; i++) {
                final int i16 = i << 4;
                long k1 = ((long) data[i16] & 0xff)
                        | (((long) data[i16 + 1] & 0xff) << 8)
                        | (((long) data[i16 + 2] & 0xff) << 16)
                        | (((long) data[i16 + 3] & 0xff) << 24)
                        | (((long) data[i16 + 4] & 0xff) << 32)
                        | (((long) data[i16 + 5] & 0xff) << 40)
                        | (((long) data[i16 + 6] & 0xff) << 48)
                        | (((long) data[i16 + 7] & 0xff) << 56);

                long k2 = ((long) data[i16 + 8] & 0xff)
                        | (((long) data[i16 + 9] & 0xff) << 8)
                        | (((long) data[i16 + 10] & 0xff) << 16)
                        | (((long) data[i16 + 11] & 0xff) << 24)
                        | (((long) data[i16 + 12] & 0xff) << 32)
                        | (((long) data[i16 + 13] & 0xff) << 40)
                        | (((long) data[i16 + 14] & 0xff) << 48)
                        | (((long) data[i16 + 15] & 0xff) << 56);

                // mix functions for k1
                k1 *= C1;
                k1 = Long.rotateLeft(k1, R1);
                k1 *= C2;
                h1 ^= k1;
                h1 = Long.rotateLeft(h1, R2);
                h1 += h2;
                h1 = h1 * M + N1;

                // mix functions for k2
                k2 *= C2;
                k2 = Long.rotateLeft(k2, R3);
                k2 *= C1;
                h2 ^= k2;
                h2 = Long.rotateLeft(h2, R1);
                h2 += h1;
                h2 = h2 * M + N2;
            }

            // tail
            long k1 = 0;
            long k2 = 0;
            int tailStart = nblocks << 4;
            switch (length - tailStart) {
                case 15:
                    k2 ^= (long) (data[tailStart + 14] & 0xff) << 48;
                case 14:
                    k2 ^= (long) (data[tailStart + 13] & 0xff) << 40;
                case 13:
                    k2 ^= (long) (data[tailStart + 12] & 0xff) << 32;
                case 12:
                    k2 ^= (long) (data[tailStart + 11] & 0xff) << 24;
                case 11:
                    k2 ^= (long) (data[tailStart + 10] & 0xff) << 16;
                case 10:
                    k2 ^= (long) (data[tailStart + 9] & 0xff) << 8;
                case 9:
                    k2 ^= (long) (data[tailStart + 8] & 0xff);
                    k2 *= C2;
                    k2 = Long.rotateLeft(k2, R3);
                    k2 *= C1;
                    h2 ^= k2;

                case 8:
                    k1 ^= (long) (data[tailStart + 7] & 0xff) << 56;
                case 7:
                    k1 ^= (long) (data[tailStart + 6] & 0xff) << 48;
                case 6:
                    k1 ^= (long) (data[tailStart + 5] & 0xff) << 40;
                case 5:
                    k1 ^= (long) (data[tailStart + 4] & 0xff) << 32;
                case 4:
                    k1 ^= (long) (data[tailStart + 3] & 0xff) << 24;
                case 3:
                    k1 ^= (long) (data[tailStart + 2] & 0xff) << 16;
                case 2:
                    k1 ^= (long) (data[tailStart + 1] & 0xff) << 8;
                case 1:
                    k1 ^= (long) (data[tailStart] & 0xff);
                    k1 *= C1;
                    k1 = Long.rotateLeft(k1, R1);
                    k1 *= C2;
                    h1 ^= k1;
            }

            // finalization
            h1 ^= length;
            h2 ^= length;

            h1 += h2;
            h2 += h1;

            h1 = fmix64(h1);
            h2 = fmix64(h2);

            h1 += h2;
            h2 += h1;

            return new long[]{h1, h2};
        }

        private static long fmix64(long h) {
            h ^= (h >>> 33);
            h *= 0xff51afd7ed558ccdL;
            h ^= (h >>> 33);
            h *= 0xc4ceb9fe1a85ec53L;
            h ^= (h >>> 33);
            return h;
        }
    }
}

