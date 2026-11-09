package sk.web.exceptions;

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

import lombok.Getter;
import sk.exceptions.JskProblem;
import sk.utils.functional.O;

@Getter
public class WebParameterException extends JskProblemExceptionWithHttpCode {
    public enum Kind {MISSING, MALFORMED}

    private final String parameterName;
    private final Kind kind;
    private final boolean body;
    private final O<String> rawValue;
    private final O<Exception> conversionFailure;

    public WebParameterException(String parameterName, Kind kind, boolean body, O<String> rawValue,
            O<Exception> conversionFailure, JskProblem problem) {
        super(400, problem, false);
        this.parameterName = parameterName;
        this.kind = kind;
        this.body = body;
        this.rawValue = rawValue;
        this.conversionFailure = conversionFailure;
    }
}
