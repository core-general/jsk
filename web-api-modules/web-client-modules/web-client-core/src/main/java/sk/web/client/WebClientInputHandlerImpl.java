package sk.web.client;

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
import lombok.NoArgsConstructor;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.functional.Op1;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebClientInputHandlerImpl implements WebClientInputHandler {
    private O<Op1<WebApiClientExecutionModel>> preRequest = O.empty();

    public static WebClientInputHandlerImpl immutable(Op1<WebApiClientExecutionModel> preRequest) {
        return new WebClientInputHandlerImpl(preRequest);
    }

    public static WebClientInputHandlerImpl mutable(C1<WebApiClientExecutionModel> preRequest) {
        return new WebClientInputHandlerImpl(preRequest);
    }

    public static WebClientInputHandlerImpl empty() {
        return new WebClientInputHandlerImpl();
    }

    private WebClientInputHandlerImpl(Op1<WebApiClientExecutionModel> preRequest) {
        this.preRequest = O.ofNull(preRequest);
    }

    private WebClientInputHandlerImpl(C1<WebApiClientExecutionModel> preRequest) {
        this.preRequest = O.ofNull(w -> {
            preRequest.accept(w);
            return w;
        });
    }

    @Override
    public O<Op1<WebApiClientExecutionModel>> preRequest() {
        return preRequest;
    }
}
