package sk.services.rand;

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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.List;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RandTextGenerator {
    IRand rnd;
    Map<Character, VocabItem> items;
    List<String> possibleSequences;

    public RandTextGenerator(List<VocabItem> items, List<String> possibleSequences, IRand rnd) {
        this.rnd = rnd;
        this.items = items.stream().collect(Cc.toM($ -> $.getSymbol(), $ -> $));
        this.possibleSequences = possibleSequences;
    }

    public String generateNext() {
        final String pattern = rnd.rndFromList(possibleSequences).get();
        StringBuilder sb = new StringBuilder();
        St.forEachChar(pattern, ch -> {
            final VocabItem item = items.get(ch);
            if (item == null) {
                sb.append(ch);
            } else {
                sb.append(rnd.rndFromList(items.get(ch).getVariations()).get());
            }
        });
        return sb.toString();
    }

    public static interface VocabItem {
        char getSymbol();

        List<String> getVariations();
    }
}
