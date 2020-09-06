package sk.web.server.filters;

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

import lombok.Value;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.context.WebRequestVariable;
import sk.web.utils.WebApiMethod;

@Value
public class WebServerFilterContext<API> {
    WebApiMethod<API> apiMethod;
    WebRequestInnerContext requestContext;
    WebServerFilterNext nextInChain;

    public final boolean allowAuxiliaryFunction(WebRequestVariable forceFunction) {
        final Boolean forceIt = requestContext.getVariableValue(forceFunction)
                .filter($ -> $.getClass() == Boolean.class)
                .map($ -> (boolean) $)
                .orElse(null);
        return forceIt != null ? forceIt : !apiMethod.isAuxiliaryMethod();
    }

    public final void addProblemHeader() {
        requestContext.addProblemHeader();
    }
}
