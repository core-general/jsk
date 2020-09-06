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

public interface Uni2AsciiDecoder {
    /**
     * Tries to transliterate unicode to 7 bit ascii
     */
    public String decodeSimple(String input);

    /**
     * Tries to transliterate unicode to 7 bit ascii with '-' instead of all other characters except ascii letters and numbers
     */
    public String decodeUrlLower(String input);
}
