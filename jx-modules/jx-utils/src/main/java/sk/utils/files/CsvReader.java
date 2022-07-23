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
import sk.utils.statics.Io;
import sk.utils.statics.St;
import sk.utils.tuples.X1;

import java.io.InputStream;
import java.util.*;

public class CsvReader {

    public static void main(String[] args) {
        final String s = "/home/kivan/home_tmp/sb/LS_ADDS_JAN_JUN22.csv";
        final InputStream inputStream = Io.bRead(s).oIs().get();
        final List<Map<String, String>> fromCsv = getFromCsv(inputStream);
        int i = 0;
    }

    public static final String COMMA = ",";

    public static List<Map<String, String>> getFromCsv(String csv) {
        return getFromCsv(csv, ",");
    }

    public static List<Map<String, String>> getFromCsv(String csv, String columnSplitter) {
        return getFromCsvUni(new SimpleCsvProcessor(new Scanner(csv), columnSplitter));
    }

    public static List<Map<String, String>> getFromCsv(InputStream is) {
        return getFromCsv(is, ",");
    }

    public static List<Map<String, String>> getFromCsv(InputStream is, String columnSplitter) {
        return getFromCsvUni(new SimpleCsvProcessor(is, columnSplitter));
    }

    public static List<Map<String, String>> getFromListList(List<List<String>> data) {
        return getFromCsvUni(new ListListCsvProcessor(data));
    }

    private static <T extends ObjectProcessor> List<Map<String, String>> getFromCsvUni(T csvObject) {
        try {
            final String[] firstLine = csvObject.getFirstLine();
            MultiBiMap<String, Integer> i2s = new MultiBiMap<>(Cc.ordering(Cc.l(firstLine)));

            List<Map<String, String>> toRet = Cc.l();
            csvObject.forEachRemaining(strings -> {
                //todo make async if needed
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
        } finally {
            try {
                csvObject.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private interface ObjectProcessor extends Iterator<String[]>, AutoCloseable {
        String[] getFirstLine();
    }

    private static class SimpleCsvProcessor implements ObjectProcessor {
        private Scanner scanner;
        private final String splitter;
        private final Iterator<String> ccsv;
        private int curIndex = 0;

        @Override
        public void close() throws Exception {
            scanner.close();
        }

        public SimpleCsvProcessor(InputStream is, String splitter) {
            this(new Scanner(is), splitter);
        }

        public SimpleCsvProcessor(Scanner scanner, String splitter) {
            this.scanner = scanner;
            this.splitter = splitter;
            ccsv = new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return scanner.hasNextLine();
                }

                @Override
                public String next() {
                    return scanner.nextLine();
                }
            };
        }

        @Override
        public String[] getFirstLine() {
            final String[] firstLine = trySplitStrings(ccsv.next());
            if (Arrays.stream(firstLine).anyMatch($ -> St.isNullOrEmpty($))) {
                throw new RuntimeException("First line malformed");
            }
            return firstLine;
        }

        @Override
        public boolean hasNext() {
            return ccsv.hasNext();
        }

        @Override
        public String[] next() {
            return trySplitStrings(ccsv.next());
        }

        private String[] trySplitStrings(String curRow) {
            try {
                return switch (splitter) {
                    case COMMA -> splitEscapedCsv(curRow);
                    default -> curRow.split(splitter);
                };
            } catch (Exception e) {
                throw new RuntimeException("Problem in line: " + (curIndex + 1) + " " + e.getMessage(), e);
            }
        }

        private String[] splitEscapedCsv(String s) {
            List<String> processedTokens = Cc.l();
            X1<CSVParseStage> stage = new X1<>(CSVParseStage.NORMAL_START);
            int[] symbolCounter = new int[]{0};
            StringBuilder curToken = new StringBuilder("");
            St.forEachChar(s, ch -> {
                symbolCounter[0]++;
                switch (stage.get()) {
                    case NORMAL_START -> {
                        switch (ch) {
                            case ',' -> finishToken(processedTokens, curToken);
                            case '"' -> stage.set(CSVParseStage.IN_STRING);
                            case ' ' -> {}
                            default -> {
                                stage.set(CSVParseStage.NORMAL_NO_QUOTE);
                                curToken.append(ch);
                            }
                        }
                    }
                    case NORMAL_NO_QUOTE -> {
                        switch (ch) {
                            case ',' -> {
                                stage.set(CSVParseStage.NORMAL_START);
                                finishToken(processedTokens, curToken);
                            }
                            case '"' -> throw error(symbolCounter, ch);
                            default -> curToken.append(ch);
                        }
                    }
                    case IN_STRING -> {
                        switch (ch) {
                            case '"' -> stage.set(CSVParseStage.IN_STRING_QUOTE);
                            default -> curToken.append(ch);
                        }
                    }
                    case IN_STRING_QUOTE -> {
                        switch (ch) {
                            case '"' -> {
                                stage.set(CSVParseStage.IN_STRING);
                                curToken.append('"');
                            }
                            case ',' -> {
                                stage.set(CSVParseStage.NORMAL_START);
                                finishToken(processedTokens, curToken);
                            }
                            case ' ' -> {
                                stage.set(CSVParseStage.IN_STRING_WAITING_SPACE_OR_COMMA);
                                finishToken(processedTokens, curToken);
                            }
                            default -> throw error(symbolCounter, ch);
                        }
                    }
                    case IN_STRING_WAITING_SPACE_OR_COMMA -> {
                        switch (ch) {
                            case ' ' -> {}
                            case ',' -> stage.set(CSVParseStage.NORMAL_START);
                            default -> throw error(symbolCounter, ch);
                        }
                    }
                }
            });
            switch (stage.get()) {
                case NORMAL_START, NORMAL_NO_QUOTE -> {}
                default -> throw error(symbolCounter, '\t');
            }

            finishToken(processedTokens, curToken);
            return processedTokens.toArray(String[]::new);
        }

        private RuntimeException error(int[] symbolCounter, char symbol) {
            return new RuntimeException(
                    "Problem with symbol number: " + symbolCounter[0] + " '" + symbol + "'");
        }

        private void finishToken(List<String> processedTokens, StringBuilder curToken) {
            processedTokens.add(curToken.toString().trim());
            curToken.setLength(0);
        }


        private enum CSVParseStage {
            NORMAL_START,// Just the normal value, if we meet ',' then we directly create new token
            NORMAL_NO_QUOTE,// Just the normal value, if we meet ',' then we directly create new token
            IN_STRING, // If we were in normal and after ',' we see a '"' then we need to parse the string fully ignoring commas
            // and exit this mode only when the last " is met before ,
            IN_STRING_QUOTE, // if we met quote while IN_STRING in the previous parse stage
            IN_STRING_WAITING_SPACE_OR_COMMA, // If we closed string, and need to wait for next comma while eating spaces
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

        @Override
        public void close() throws Exception {}
    }
}
