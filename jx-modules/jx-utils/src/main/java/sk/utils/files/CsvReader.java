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

import sk.utils.collections.MultiBiMap;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CsvReader {
    public static List<Map<String, String>> getFromCsv(String csv) {
        return getFromCsv(csv, ",");
    }

    public static List<Map<String, String>> getFromCsv(String csv, String columnSplitter) {
        return getFromCsvUni(new SimpleCsvProcessor(csv, columnSplitter));
    }

    public static List<Map<String, String>> getFromListList(List<List<String>> data) {
        return getFromCsvUni(new ListListCsvProcessor(data));
    }

    private static <T extends ObjectProcessor> List<Map<String, String>> getFromCsvUni(T csvObject) {
        final String[] firstLine = csvObject.getFirstLine();
        MultiBiMap<String, Integer> i2s = new MultiBiMap<>(Cc.ordering(Cc.l(firstLine)));

        List<Map<String, String>> toRet = Cc.l();
        csvObject.forEachRemaining(strings -> {
            Map<String, String> toAdd = Cc.m();
            for (int j = 0; j < strings.length; j++) {
                String datum = strings[j];
                toAdd.put(i2s.getSecondByFirst().get(j).iterator().next(), St.isNullOrEmpty(datum) ? null : datum);
            }

            for (int i = strings.length; i < firstLine.length; i++) {
                toAdd.put(i2s.getSecondByFirst().get(i).iterator().next(), null);
            }
            toRet.add(toAdd);
        });

        return toRet;
    }

    private interface ObjectProcessor extends Iterator<String[]> {
        String[] getFirstLine();
    }

    private static class SimpleCsvProcessor implements ObjectProcessor {
        private final String text;
        private final String splitter;
        private final String[] ccsv;
        private int curIndex = 0;

        public SimpleCsvProcessor(String text, String splitter) {
            this.text = text;
            this.splitter = splitter;
            ccsv = this.text.split("\n");
        }

        @Override
        public String[] getFirstLine() {
            return ccsv[0].trim().split(splitter);
        }

        @Override
        public boolean hasNext() {
            return 1 + curIndex < ccsv.length;
        }

        @Override
        public String[] next() {
            curIndex++;
            return ccsv[curIndex].split(splitter);
        }
    }

    private static class ListListCsvProcessor implements ObjectProcessor {
        private final List<List<String>> text;
        private int curIndex = 0;

        public ListListCsvProcessor(List<List<String>> text) {
            this.text = text;
        }

        @Override
        public String[] getFirstLine() {
            return text.get(0).toArray(String[]::new);
        }

        @Override
        public boolean hasNext() {
            return 1 + curIndex < text.size();
        }

        @Override
        public String[] next() {
            curIndex++;
            return text.get(curIndex).toArray(String[]::new);
        }
    }
}
