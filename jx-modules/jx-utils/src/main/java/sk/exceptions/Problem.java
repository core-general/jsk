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
import lombok.Value;
import sk.utils.functional.O;

/**
 * Just class to express problem, not to pass Strings in Optionals etc.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Problem {
    String text;
    O<Exception> exception;

    public static Problem of(String txt) {
        return new Problem(txt, O.empty());
    }

    public static Problem of(String txt, Exception e) {
        return new Problem(txt, O.ofNull(e));
    }

    public static Problem of(Exception e) {
        return new Problem(e.getMessage(), O.ofNull(e));
    }
}
