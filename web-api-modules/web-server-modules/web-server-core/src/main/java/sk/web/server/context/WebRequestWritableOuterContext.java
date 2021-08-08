package sk.web.server.context;

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
import sk.utils.functional.O;
import sk.web.redirect.WebRedirectResult;
import sk.web.renders.WebRender;
import sk.web.renders.WebRenderResult;
import sk.web.renders.WebReply;
import sk.web.utils.WebApiMethod;


public abstract class WebRequestWritableOuterContext {
    public abstract void redirect(String url);

    public abstract void setCookie(String key, String value, int seconds);

    public abstract void setResponseHeader(String key, String value);

    protected abstract void innerSetResponse(WebRenderResult result, O<WebRedirectResult> redirect);

    public final void addProblemHeader() {
        setResponseHeader(JskProblem.PROBLEM_SIGN, "+");
    }

    public final void setError(int responseCode, WebRender errorRender, JskProblem problem, WebApiMethod<?> method) {
        setResponse(errorRender.getResult(WebReply.problem(responseCode, problem), errorRender, method), O.empty());
    }

    public final void setResponse(WebRenderResult result, O<WebRedirectResult> redirect) {
        if (result.getMeta().isProblem()) {
            addProblemHeader();
        }
        innerSetResponse(result, redirect);
    }
}
