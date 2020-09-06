package sk.mvn.model;

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
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.utils.functional.O;
import sk.utils.statics.St;

import java.util.List;


@Getter
@NoArgsConstructor
public class ApiMethodModel {

    String mName;
    String methodFullPath;
    O<String> comment = O.empty();
    List<ApiExceptionInfo> exceptions;
    O<ApiFieldOrParameterModel> returnInfo = O.empty();
    List<ApiFieldOrParameterModel> params;
    @Setter String nameAndParamHash;

    public ApiMethodModel(String methodName,
            String methodFullPath,
            O<String> comment, List<ApiExceptionInfo> exceptions,
            O<ApiFieldOrParameterModel> returnInfo, List<ApiFieldOrParameterModel> params) {
        this.mName = methodName;
        this.methodFullPath = methodFullPath;
        this.comment = comment;
        this.exceptions = exceptions;
        this.returnInfo = returnInfo;
        this.params = params;
    }

    public String getCommentOrNull() {
        return comment.map($ -> $.replace("\n\n", "*\n⭐")).filter(St::isNotNullOrEmpty).orElse(null);
    }

    public ApiFieldOrParameterModel getReturnInfoOrVoid() {
        return returnInfo.orElseGet(() -> new ApiFieldOrParameterModel("", O.empty(), O.of("void")));
    }
}
