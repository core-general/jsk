package sk.utils.files;

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

import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvWriter {
    public static String toCsv(List<Map<String, ?>> listOfFieldNamesAndValues) {
        return toCsv(listOfFieldNamesAndValues,
                listOfFieldNamesAndValues.stream().flatMap($ -> $.keySet().stream()).distinct().sorted().collect(
                        Collectors.toList()), ",");
    }

    public static String toCsv(
            List<Map<String, ?>> listOfFieldNamesAndValues,
            List<String> columnOrder
    ) {
        return toCsv(listOfFieldNamesAndValues, columnOrder, ",");
    }


    public static String toCsv(
            List<Map<String, ?>> listOfFieldNamesAndValues,
            List<String> columnOrder,
            String columnSeparator
    ) {

        Set<String> co = new HashSet<>(columnOrder);
        Set<String> allColumns = listOfFieldNamesAndValues.stream()
                .flatMap($ -> $.keySet().stream()).collect(Collectors.toSet());

        if (!(co.containsAll(allColumns) && allColumns.containsAll(co) && co.size() == allColumns.size())) {
            throw new RuntimeException(String.format("Problem with: co=%s\nallColumns=%s", Cc.join(co), Cc.join(allColumns)));
        }

        String firstLine = Cc.join(columnSeparator, columnOrder);
        final Comparator<String> columnNameComparator = Cc.orderingComparator(Cc.ordering(columnOrder));


        final String csvData = Stream.concat(Stream.of(firstLine), listOfFieldNamesAndValues.stream()
                        .map($ -> columnOrder.stream()
                                .map($$ -> O.ofNull($.get($$)).map($$$ -> $$$.toString()).orElse(""))
                                .collect(Collectors.joining(columnSeparator))))
                .collect(Collectors.joining("\n"));
        return csvData;
    }
}
