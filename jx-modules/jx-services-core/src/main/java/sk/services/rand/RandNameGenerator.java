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

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandNameGenerator extends RandTextGenerator {
    private int nameParts;

    public RandNameGenerator(IRand rnd, int nameParts) {
        super(Cc.l(Vocals.values()), Cc.l("vd", "cvdvd", "cvd", "vdvd"), rnd);
        this.nameParts = nameParts;
    }

    @Override
    public String generateNext() {
        return IntStream.range(0, nameParts).mapToObj($ -> St.capFirst(super.generateNext())).collect(Collectors.joining(" "));
    }

    @Getter
    @AllArgsConstructor
    private enum Vocals implements RandTextGenerator.VocabItem {
        vocals('v', Cc.l("a", "e", "i", "o", "u", "ei", "ai", "ou", "j",
                "ji", "y", "oi", "au", "oo")),
        startConnosant('c', Cc.l("b", "c", "d", "f", "g", "h", "k",
                "l", "m", "n", "p", "q", "r", "s", "t", "v", "w", "x", "z",
                "ch", "bl", "br", "fl", "gl", "gr", "kl", "pr", "st", "sh",
                "th")),
        endConnosant('d', Cc.l("b", "d", "f", "g", "h", "k", "l", "m",
                "n", "p", "r", "s", "t", "v", "w", "z", "ch", "gh", "nn", "st",
                "sh", "th", "tt", "ss", "pf", "nt"));
        char symbol;
        List<String> variations;
    }
}
