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


import sk.exceptions.JskProblem;
import sk.exceptions.JskProblemException;
import sk.services.except.IExceptBase;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRender;

public interface IWebExcept extends IExceptBase {
    WebRender getDefaultExceptionRender();

    //region Missing parameter
    default WebFilterOutput returnMissingParameter(String paramName, boolean param) {
        return WebFilterOutput.rawProblem(400, getMissingParameterProblem(paramName, param));
    }

    default <T> T throwMissingParameter(String paramName, boolean param) {
        throw new JskProblemExceptionWithHttpCode(400, getMissingParameterProblem(paramName, param),
                haveStackTrace(JskProblemException.class));
    }


    default JskProblem getMissingParameterProblem(String paramName, boolean param) {
        final String headerOrParam = param ? "param" : "header";
        return JskProblem.substatus("missing_" + headerOrParam, headerOrParam + ": " + paramName + " must exist");
    }
    //endregion

    //region Must retry
    default WebFilterOutput returnMustRetry(String reason) {
        return WebFilterOutput.rawProblem(503, getMustRetry(reason));
    }

    default <T> T throwMustRetry(String reason) {
        throw new JskProblemExceptionWithHttpCode(503, getMustRetry(reason),
                haveStackTrace(JskProblemException.class));
    }


    default JskProblem getMustRetry(String reason) {
        return JskProblem.substatus("must_retry", reason);
    }
    //endregion


    default <T> T throwByDescription(int httpCode, String s) {
        throw new JskProblemExceptionWithHttpCode(httpCode, JskProblem.description(s), haveStackTrace(JskProblemException.class));
    }

    default <T, A extends Enum<A>> T throwByCode(int httpCode, A code) {
        throw new JskProblemExceptionWithHttpCode(httpCode, JskProblem.code(code), haveStackTrace(JskProblemException.class));
    }

    default <T> T throwByCode(int httpCode, String code) {
        throw new JskProblemExceptionWithHttpCode(httpCode, JskProblem.code(code), haveStackTrace(JskProblemException.class));
    }

    default <T, A extends Enum<A>> T throwBySubstatus(int httpCode, A code, String substatus) {
        throw new JskProblemExceptionWithHttpCode(httpCode, JskProblem.substatus(code, substatus), haveStackTrace(
                JskProblemException.class));
    }

    default <T> T throwBySubstatus(int httpCode, String code, String substatus) {
        throw new JskProblemExceptionWithHttpCode(httpCode, JskProblem.substatus(code, substatus), haveStackTrace(
                JskProblemException.class));
    }
}
