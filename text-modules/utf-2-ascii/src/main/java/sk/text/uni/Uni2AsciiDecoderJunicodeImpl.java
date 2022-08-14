package sk.text.uni;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import net.gcardone.junidecode.JunidecodeFixed;
import sk.utils.javafixes.BadCharReplacer;
import sk.utils.statics.St;

public class Uni2AsciiDecoderJunicodeImpl implements Uni2AsciiDecoder {
//    private final static char[] CHAR_TABLE = calculateTable();

    public static final String BAD_CHAR = "-";
    public static final String DOUBLE_BAD_CHAR = "--";
    public static final String TRIPLE_BAD_CHAR = "---";
    private final static BadCharReplacer bcr = BadCharReplacer.bitSetReplacer(St.engENGDig + BAD_CHAR);

    @Override
    public String decodeSimple(String input) {
        return JunidecodeFixed.unidecode(input);
    }

    @Override
    public String decodeUrlLower(String input) {
        input = decodeSimple(input);
        return bcr.replaceChars(input, BAD_CHAR)
                .replace(TRIPLE_BAD_CHAR, BAD_CHAR)
                .replace(DOUBLE_BAD_CHAR, BAD_CHAR);
    }
}
