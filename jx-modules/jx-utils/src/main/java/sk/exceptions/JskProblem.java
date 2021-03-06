package sk.exceptions;

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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sk.utils.functional.O;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class JskProblem {
    public final static String PROBLEM_SIGN = "__problem";
    private final String __problem = "+";
    private O<String> code = O.empty();
    private O<String> substatus = O.empty();
    private O<String> description = O.empty();

    public static JskProblem code(String code) {
        return new JskProblem(O.of(code), O.empty(), O.empty());
    }

    public static <A extends Enum<A>> JskProblem code(A code) {
        return code(code.name());
    }

    public static JskProblem substatus(String code, String substatus) {
        return new JskProblem(O.of(code), O.of(substatus), O.empty());
    }

    public static <A extends Enum<A>> JskProblem substatus(A code, String substatus) {
        return substatus(code.name(), substatus);
    }

    public static JskProblem description(String description) {
        return new JskProblem(O.empty(), O.empty(), O.of(description));
    }

    public String getCode() {
        return code.orElse(null);
    }

    public String getSubstatus() {
        return substatus.orElse(null);
    }

    public String getDescription() {
        return description.orElse(null);
    }

    @Override
    public String toString() {
        return description != null && description.isPresent()
               ? description.get()
               : (code != null && code.isPresent() ? "code='" + code.get() + "'" : "") +
                       (substatus != null && substatus.isPresent() ? ", subStatus='" + substatus.get() + '\'' : "");
    }
}
