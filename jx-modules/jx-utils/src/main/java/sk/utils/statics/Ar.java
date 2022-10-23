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

import sk.utils.functional.F1;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;

public class Ar/*rays*/ {

    public static double[] mapAll(F1<double[], Double> summarizer, boolean forceSameSize, double[]... doubles) {
        IntSummaryStatistics sizes = Arrays.stream(doubles).mapToInt($ -> $.length).distinct().summaryStatistics();
        if (forceSameSize && sizes.getCount() > 1) {
            throw new RuntimeException("Different sizes: " + sizes);
        }

        double[] toRet = new double[sizes.getMax()];
        double[] curParams = new double[doubles.length];
        for (int i = 0; i < toRet.length; i++) {
            for (int j = 0; j < doubles.length; j++) {
                curParams[j] = indexOr0(doubles[j], i);
            }
            toRet[i] = summarizer.apply(curParams);
        }
        return toRet;
    }

    public static double[] map(double[] toMap, DoubleUnaryOperator operator) {
        for (int i = 0; i < toMap.length; i++) {
            toMap[i] = operator.applyAsDouble(toMap[i]);
        }
        return toMap;
    }

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

    public static double avg(double[] coreLoad) {
        return Arrays.stream(coreLoad).average().orElse(0);
    }

    public static double sum(double[] coreLoad) {
        return Arrays.stream(coreLoad).sum();
    }

    public static double lastOr0(double[] coreLoad) {
        return coreLoad.length == 0 ? 0 : coreLoad[coreLoad.length - 1];
    }

    public static double indexOr0(double[] coreLoad, int index) {
        return index < coreLoad.length ? coreLoad[index] : 0;
    }

    public static double[] sortCopy(double[] coreLoad) {
        final double[] copy = Arrays.copyOf(coreLoad, coreLoad.length);
        Arrays.sort(copy);

        return copy;
    }
}
