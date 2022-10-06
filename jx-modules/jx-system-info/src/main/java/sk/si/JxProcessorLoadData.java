package sk.si;/*-
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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @param sortedLoadByCore sorted ASCENDING
 * @param coreDeviation    Standard deviation for cores
 */
public record JxProcessorLoadData(double avgProcessorLoad, double[] loadByCore, double[] sortedLoadByCore, double coreDeviation) {
    @Override
    public String toString() {
        return """
                %.3f avg
                %.3f s.deviation
                [%s] core values
                [%s] core ascending sorted values"""
                .formatted(avgProcessorLoad,
                        coreDeviation,
                        Arrays.stream(loadByCore).mapToObj($ -> "%.3f".formatted($)).collect(Collectors.joining(", ")),
                        Arrays.stream(sortedLoadByCore).mapToObj($ -> "%.3f".formatted($)).collect(Collectors.joining(", ")));
    }
}
