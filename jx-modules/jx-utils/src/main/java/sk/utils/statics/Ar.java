package sk.utils.statics;

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
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

public class Ar {

    public static double[] getValuesIncrementedBy1(int count) {
        return IntStream.range(0, count).mapToDouble($ -> $).toArray();
    }

    public static double[] fill(int size, double value) {
        double[] arr = new double[size];
        Arrays.fill(arr, value);
        return arr;
    }

    public static boolean[] fill(int size, boolean value) {
        boolean[] arr = new boolean[size];
        Arrays.fill(arr, value);
        return arr;
    }

    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] longToByteArray(long value) {
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    public static int bArrToInt(byte[] value) {
        return ByteBuffer.wrap(value).getInt();
    }

    public static long bArrToLong(byte[] value) {
        return ByteBuffer.wrap(value).getLong();
    }

    public static byte[] intToByteArray(int[] vals) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(vals.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(vals);

        return byteBuffer.array();
    }

    public static int[] byteToIntArray(byte[] vals) {
        if (vals.length % 4 != 0) {
            throw new RuntimeException("Not int[]!");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(vals);
        final int size = vals.length / 4;
        int[] ints = new int[size];
        for (int i = 0; i < size; i++) {
            ints[i] = byteBuffer.getInt(4 * i);
        }
        return ints;
    }
}
