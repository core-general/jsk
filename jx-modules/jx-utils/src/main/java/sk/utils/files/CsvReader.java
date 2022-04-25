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

import java.util.List;
import java.util.Map;

public class CsvReader {
    public static List<Map<String, String>> getFromCsv(String csv) {
        final String[] ccsv = csv.split("\n");
        MultiBiMap<String, Integer> i2s = new MultiBiMap<>(Cc.ordering(Cc.l(ccsv[0].trim().split(","))));

        List<Map<String, String>> toRet = Cc.l();
        for (int i = 1; i < ccsv.length; i++) {
            String s = ccsv[i];
            final String[] data = s.split(",");
            Map<String, String> toAdd = Cc.m();
            for (int j = 0; j < data.length; j++) {
                String datum = data[j];
                toAdd.put(i2s.getSecondByFirst().get(j).iterator().next(), datum);
            }
            toRet.add(toAdd);
        }
        return toRet;
    }
}
