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

import jakarta.inject.Inject;
import sk.exceptions.JskProblem;
import sk.services.except.IExcept;
import sk.services.json.IJson;
import sk.utils.functional.OneOf;

public abstract class WebClientResultHandlerBase implements WebClientResultHandler<JskProblem> {
    protected @Inject IJson json;
    protected @Inject IExcept except;

    public abstract <T> T ifNotBusinessProblemAndNotByteArr(String val, WebRequestResultModel<T> resultModel);

    public <T> OneOf<T, JskProblem> ifBusinessProblem(WebRequestResultModel<T> resultModel) {
        return OneOf.right(json.from(resultModel.getResult().newAsString(), JskProblem.class));
    }

    public <T> OneOf<T, JskProblem> ifNotBusinessProblem(WebRequestResultModel<T> resultModel) {
        if (resultModel.getResultClass().getType() == byte[].class) {
            return OneOf.left((T) resultModel.getResult().getAsBytes());
        } else if (resultModel.getResultClass().getType() == void.class) {
            return OneOf.left((T) void.class);
        } else if (resultModel.getResultClass().getType() == String.class) {
            return OneOf.left((T) resultModel.getResult().newAsString());
        } else {
            return OneOf.left(ifNotBusinessProblemAndNotByteArr(resultModel.getResult().newAsString(), resultModel));
        }
    }

    @Override
    public final <T> OneOf<T, JskProblem> processResult(WebRequestResultModel<T> resultModel) {
        if (isProblem(resultModel)) {
            return ifBusinessProblem(resultModel);
        } else {
            return ifNotBusinessProblem(resultModel);
        }
    }

    @Override
    public <T> T doInCaseOfProblem(WebRequestResultModel<T> resultModel, JskProblem problem) {
        return except.throwByProblem(problem);
    }

    public <T> boolean isProblem(WebRequestResultModel<T> resultModel) {
        return resultModel.getResult().isBusinessProblem();
    }
}
