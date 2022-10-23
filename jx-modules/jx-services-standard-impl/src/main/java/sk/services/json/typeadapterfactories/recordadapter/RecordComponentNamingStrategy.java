package sk.services.json.typeadapterfactories.recordadapter;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import com.google.gson.FieldNamingPolicy;

import java.lang.reflect.RecordComponent;
import java.util.Locale;
/*
https://github.com/Marcono1234/gson-record-type-adapter-factory
MIT License

Copyright (c) 2021 Marcono1234

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * Strategy for transforming the name of a Record component to a JSON property name.
 *
 * <p>This interface offers multiple existing implementations, such as {@link #LOWER_CASE_WITH_UNDERSCORES},
 * as well as {@link #fromFieldNamingPolicy(FieldNamingPolicy)} which makes integration with existing
 * Gson usage easier.
 *
 * <p>Naming strategy implementations must be thread-safe.
 *
 * @see marcono1234.gson.recordadapter.RecordTypeAdapterFactory.Builder#withComponentNamingStrategy(RecordComponentNamingStrategy)
 */
public interface RecordComponentNamingStrategy {
    /**
     * Uses the Record component name as is.
     *
     * @see FieldNamingPolicy#IDENTITY
     */
    RecordComponentNamingStrategy IDENTITY = new RecordComponentNamingStrategy() {
        @Override
        public String translateName(RecordComponent component) {
            return component.getName();
        }

        @Override
        public String toString() {
            return "IDENTITY";
        }
    };

    /**
     * Converts the first letter of the Record component name to upper case.
     *
     * @see FieldNamingPolicy#UPPER_CAMEL_CASE
     */
    RecordComponentNamingStrategy UPPER_CAMEL_CASE = new RecordComponentNamingStrategy() {
        @Override
        public String translateName(RecordComponent component) {
            return uppercaseFirstLetter(component.getName());
        }

        @Override
        public String toString() {
            return "UPPER_CAMEL_CASE";
        }
    };

    /**
     * Splits the Record component name at all existing upper case characters using spaces
     * and coverts the first letter to upper case.
     *
     * @see FieldNamingPolicy#UPPER_CAMEL_CASE_WITH_SPACES
     */
    RecordComponentNamingStrategy UPPER_CAMEL_CASE_WITH_SPACES = new RecordComponentNamingStrategy() {
        @Override
        public String translateName(RecordComponent component) {
            return uppercaseFirstLetter(separateCamelCase(component.getName(), ' '));
        }

        @Override
        public String toString() {
            return "UPPER_CAMEL_CASE_WITH_SPACES";
        }
    };

    /**
     * Splits the Record component name at all existing upper case characters using underscores
     * and coverts the name to lower case.
     *
     * @see FieldNamingPolicy#LOWER_CASE_WITH_UNDERSCORES
     */
    RecordComponentNamingStrategy LOWER_CASE_WITH_UNDERSCORES = new RecordComponentNamingStrategy() {
        @Override
        public String translateName(RecordComponent component) {
            return lowercase(separateCamelCase(component.getName(), '_'));
        }

        @Override
        public String toString() {
            return "LOWER_CASE_WITH_UNDERSCORES";
        }
    };

    /**
     * Splits the Record component name at all existing upper case characters using dashes
     * and coverts the name to lower case.
     *
     * @see FieldNamingPolicy#LOWER_CASE_WITH_DASHES
     */
    RecordComponentNamingStrategy LOWER_CASE_WITH_DASHES = new RecordComponentNamingStrategy() {
        @Override
        public String translateName(RecordComponent component) {
            return lowercase(separateCamelCase(component.getName(), '-'));
        }

        @Override
        public String toString() {
            return "LOWER_CASE_WITH_DASHES";
        }
    };

    /**
     * Splits the Record component name at all existing upper case characters using dots
     * and coverts the name to lower case.
     *
     * @see FieldNamingPolicy#LOWER_CASE_WITH_DOTS
     */
    RecordComponentNamingStrategy LOWER_CASE_WITH_DOTS = new RecordComponentNamingStrategy() {
        @Override
        public String translateName(RecordComponent component) {
            return lowercase(separateCamelCase(component.getName(), '.'));
        }

        @Override
        public String toString() {
            return "LOWER_CASE_WITH_DOTS";
        }
    };

    // Based on com.google.gson.FieldNamingPolicy.upperCaseFirstLetter, but with
    // https://github.com/google/gson/issues/1965 being fixed
    private static String uppercaseFirstLetter(String s) {
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    return s;
                }

                char uppercased = Character.toUpperCase(c);
                // For leading letter only need one substring
                if (i == 0) {
                    return uppercased + s.substring(1);
                } else {
                    return s.substring(0, i) + uppercased + s.substring(i + 1);
                }
            }
        }

        return s;
    }

    // Based on com.google.gson.FieldNamingPolicy.separateCamelCase
    private static String separateCamelCase(String s, char separator) {
        StringBuilder sb = new StringBuilder();
        int length = s.length();

        int nextSectionIndex = 0;
        // Can start at 1 because won't add leading separator
        for (int i = 1; i < length; i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                sb.append(s, nextSectionIndex, i);
                sb.append(separator);
                nextSectionIndex = i;
            }
        }

        // Found nothing to split
        if (nextSectionIndex == 0) {
            return s;
        }
        // Add trailing characters
        sb.append(s.substring(nextSectionIndex));
        return sb.toString();
    }

    private static String lowercase(String s) {
        // Locale.ENGLISH to match com.google.gson.FieldNamingPolicy behavior
        return s.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Gets the Record component naming strategy which corresponds to the given Gson
     * {@link FieldNamingPolicy}.
     *
     * @param policy Gson field naming policy
     * @return The corresponding Record component naming strategy
     * @throws IllegalArgumentException If Gson adds a {@code FieldNamingPolicy} in the future which is not supported
     *                                  by this method yet
     */
    static RecordComponentNamingStrategy fromFieldNamingPolicy(FieldNamingPolicy policy) throws IllegalArgumentException {
        return switch (policy) {
            case IDENTITY -> IDENTITY;
            case UPPER_CAMEL_CASE -> UPPER_CAMEL_CASE;
            case UPPER_CAMEL_CASE_WITH_SPACES -> UPPER_CAMEL_CASE_WITH_SPACES;
            case LOWER_CASE_WITH_UNDERSCORES -> LOWER_CASE_WITH_UNDERSCORES;
            case LOWER_CASE_WITH_DASHES -> LOWER_CASE_WITH_DASHES;
            case LOWER_CASE_WITH_DOTS -> LOWER_CASE_WITH_DOTS;
            // In case Gson ever adds new policies
            //noinspection UnnecessaryDefault
            default -> throw new IllegalArgumentException("Unsupported field naming policy " + policy);
        };
    }

    /**
     * Transforms the name of a Record component.
     *
     * @param component Record component whose name should be transformed
     * @return The transformed name
     */
    String translateName(RecordComponent component);
}
