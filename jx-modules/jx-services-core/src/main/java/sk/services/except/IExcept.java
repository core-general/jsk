package sk.services.except;

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

import sk.exceptions.JskProblem;
import sk.exceptions.JskProblemException;

public interface IExcept extends IExceptBase {
    default <T> T throwByProblem(JskProblem problem) {
        throw new JskProblemException(problem, haveStackTrace(JskProblemException.class));
    }

    default <T> T throwByDescription(String s) {
        throw new JskProblemException(JskProblem.description(s), haveStackTrace(JskProblemException.class));
    }

    default <T, A extends Enum<A>> T throwByCode(A code) {
        throw new JskProblemException(JskProblem.code(code), haveStackTrace(JskProblemException.class));
    }

    default <T> T throwByCode(String code) {
        throw new JskProblemException(JskProblem.code(code), haveStackTrace(JskProblemException.class));
    }

    default <T, A extends Enum<A>> T throwBySubstatus(A code, String substatus) {
        throw new JskProblemException(JskProblem.substatus(code, substatus), haveStackTrace(JskProblemException.class));
    }

    default <T> T throwBySubstatus(String code, String substatus) {
        throw new JskProblemException(JskProblem.substatus(code, substatus), haveStackTrace(JskProblemException.class));
    }
}
