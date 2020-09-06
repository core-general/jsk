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
import sk.utils.statics.St;

public class Uni2AsciiDecoderJunicodeImpl implements Uni2AsciiDecoder {
    private final static char[] CHAR_TABLE = calculateTable();

    @Override
    public String decodeSimple(String input) {
        return JunidecodeFixed.unidecode(input);
    }

    @Override
    public String decodeUrlLower(String input) {
        input = decodeSimple(input);
        char[] chars = new char[input.length()];

        boolean started = false;
        boolean lastWasMinus = false;

        for (int i = 0, j = 0; i < input.length(); i++) {
            final char c = input.charAt(i);

            char possibleChar = c < 128 ? CHAR_TABLE[c] : '-';
            if (possibleChar == '\'') {

            } else if (possibleChar == '-') {
                if (started && !lastWasMinus) {
                    chars[j++] = possibleChar;
                    lastWasMinus = true;
                }
            } else {
                started = true;
                lastWasMinus = false;
                chars[j++] = possibleChar;
            }
        }

        for (int i = chars.length - 1; i > 0; i--) {
            if (chars[i] == '-' || chars[i] == 0) {
                chars[i] = ' ';
            } else {
                break;
            }
        }

        return new String(chars).trim().toLowerCase();
    }


    private static char[] calculateTable() {
        final String possibleChars = St.engENGDig + "-'";
        final int count = 128;
        char[] toret = new char[count];
        for (int i = 0; i < count; i++) {
            if (possibleChars.contains(((char) i) + "")) {
                toret[i] = (char) i;
            } else {
                toret[i] = '-';
            }
        }
        return toret;
    }

    public static void main(String[] args) {
        final String phrase =
                "コロナウイルスフルクラップ";
        System.out.println(new Uni2AsciiDecoderJunicodeImpl().decodeUrlLower(phrase));
        System.out.println(new Uni2AsciiDecoderJunicodeImpl().decodeSimple(phrase));
    }
}
