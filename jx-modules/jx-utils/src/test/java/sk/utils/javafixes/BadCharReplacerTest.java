package sk.utils.javafixes;

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sk.utils.statics.St;

public class BadCharReplacerTest {

    @Test
    public void bitSetReplacer() {
        final BadCharReplacer badCharReplacer = BadCharReplacer.bitSetReplacer(St.engENGDig + "❗");
        final String s = badCharReplacer.replaceChars("💎💎a🧿aa❗B☎B11❗", "🧿x");
        Assertions.assertEquals(s, "🧿x🧿xa🧿xaa❗B🧿xB11❗");
    }

    @Test
    public void arrReplacer() {
        final BadCharReplacer badCharReplacer = BadCharReplacer.arrReplacer(St.engENGDig + "❗");
        final String s = badCharReplacer.replaceChars("💎💎a🧿aa❗B☎B11❗", "🧿x");
        Assertions.assertEquals(s, "🧿x🧿xa🧿xaa❗B🧿xB11❗");
    }

    @Test
    public void hashSetReplacer() {
        final BadCharReplacer badCharReplacer = BadCharReplacer.hashSetReplacer(St.engENGDig + "❗");
        final String s = badCharReplacer.replaceChars("💎💎a🧿aa❗B☎B11❗", "🧿x");
        Assertions.assertEquals(s, "🧿x🧿xa🧿xaa❗B🧿xB11❗");
    }
}
