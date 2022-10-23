package sk.utils.statics;

import lombok.AllArgsConstructor;
import sk.utils.functional.F2;
import sk.utils.functional.O;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.*;

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
@SuppressWarnings({"unused"})
public final class Ma/*ths*/ {
    public static O<Long> median(List<Long> longs) {
        return medianN(longs, Long::compareTo, (a, b) -> (a + b) / 2);
    }

    public static O<Double> medianD(List<Double> doubles) {
        return medianN(doubles, Double::compareTo, (a, b) -> (a + b) / 2);
    }

    public static float mean(float v1, float v2) {return (v1 + v2) / 2;}

    public static double mean(double v1, double v2) {return (v1 + v2) / 2;}

    public static long clamp(long value, long min, long max) {
        return value < min ? min : (min(value, max));
    }

    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            value = 0;
        }
        if (Double.isInfinite(value)) {
            return signum(value) < 0 ? min : max;
        }

        return value < min ? min : min(value, max);
    }

    public static boolean inside(int value, int min, int max) {return value >= min && value <= max;}

    public static boolean inside(double value, double min, double max) {return value >= min && value <= max;}

    public static Double rand(Number min, Number max) {
        return (max.doubleValue() - min.doubleValue()) * random() + min.doubleValue();
    }

    public static boolean isInt(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isFloat(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        boolean dot = false;
        for (int i = 0; i < value.length(); i++) {
            final char ch = value.charAt(i);

            if (Character.isDigit(ch)) {
                continue;
            }
            if (ch == '.') {
                if (i == 0 || i == value.length() - 1) {
                    return false;
                }
                if (!dot) {
                    dot = true;
                    continue;
                } else {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    @AllArgsConstructor
    public enum SampleSizeAccuracy {
        _85(1.44), _90(1.65), _95(1.96), _97(2.18), _99(2.58), _99_7(3.00);
        double Z;
    }

    /**
     * @param allowedErrorPercent 0.0-1.0
     * @return
     */
    public static int optimalSampleSize(SampleSizeAccuracy accuracy, double allowedErrorPercent, O<Long> fullSize) {
        double SS = ((accuracy.Z * accuracy.Z) * (0.5) * (0.5)) / (allowedErrorPercent * allowedErrorPercent);
        return (int) round(fullSize.map($ -> SS / (1 + (SS - 1) / $)).orElse(SS));
    }

    /**
     * @return
     */
    public static double errorRateOfSample(SampleSizeAccuracy accuracy, int partialSampleSize, O<Long> fullSize) {
        return accuracy.Z *
                sqrt((0.5 * 0.5 / partialSampleSize) * fullSize.map($ -> (1d * $ - partialSampleSize) / ($ - 1)).orElse(1d));
    }

    //region parsing
    public static float pf(String s) {
        return Float.parseFloat(s);
    }

    public static boolean pb(String s) {
        return Boolean.parseBoolean(s);
    }

    public static double pd(String s) {
        return Double.parseDouble(s);
    }

    public static int pi(String s) {
        return Integer.parseInt(s);
    }

    public static long pl(String s) {
        return Long.parseLong(s);
    }

    public static O<Float> opf(String s) {
        try {
            return isFloat(s) ? O.of(pf(s)) : O.empty();
        } catch (NumberFormatException e) {
            return O.empty();
        }
    }

    public static O<Boolean> opb(String s) {
        try {
            return O.of(pb(s));
        } catch (NumberFormatException e) {
            return O.empty();
        }
    }

    public static O<Double> opd(String s) {
        try {
            return isFloat(s) ? O.of(pd(s)) : O.empty();
        } catch (NumberFormatException e) {
            return O.empty();
        }
    }

    public static O<Integer> opi(String s) {
        try {
            return isInt(s) ? O.of(pi(s)) : O.empty();
        } catch (NumberFormatException e) {
            return O.empty();
        }
    }

    public static O<Long> opl(String s) {
        try {
            return isInt(s) ? O.of(pl(s)) : O.empty();
        } catch (NumberFormatException e) {
            return O.empty();
        }
    }
    //endregion

    //region Private
    private Ma() {}


    private static <T extends Number> O<T> medianN(List<T> numbers, Comparator<T> comparator, F2<T, T, T> averager) {
        Collections.sort(numbers, comparator);
        if (numbers.size() == 0) {
            return O.empty();
        }
        if (numbers.size() == 1) {
            return Cc.first(numbers);
        }
        if (numbers.size() % 2 == 1) {
            return Cc.getAt(numbers, numbers.size() / 2);
        } else {
            return O.of(averager.apply(Cc.getAt(numbers, (numbers.size() / 2) - 1).get(),
                    Cc.getAt(numbers, (numbers.size() / 2)).get()));
        }
    }
    //endregion
}
