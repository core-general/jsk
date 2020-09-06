package sk.utils.statics;

import static java.lang.Math.min;
import static java.lang.Math.random;

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
public final class Ma {
    public static float mean(float v1, float v2) {return (v1 + v2) / 2;}

    public static double mean(double v1, double v2) {return (v1 + v2) / 2;}

    public static long clamp(long value, long min, long max) {return value < min ? min : (min(value, max));}

    public static double clamp(double value, double min, double max) {
        return value < min ? min : (min(value, max));
    }

    public static boolean inside(int value, int min, int max) {return !(value < min || value > max);}

    public static Double rand(Number min, Number max) {
        return (max.doubleValue() - min.doubleValue()) * random() + min.doubleValue();
    }

    public static String miliToS(int millival) {
        return millival / 100 + "." + millival % 100;
    }

    public static Integer sToMill(String val) {
        int[] ints = Cc.stream(val.split("\\.")).mapToInt(Ma::pi).toArray();
        return ints[0] * 100 + ints[1];
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
    //endregion

    //region Private
    private Ma() {}
    //endregion
}
