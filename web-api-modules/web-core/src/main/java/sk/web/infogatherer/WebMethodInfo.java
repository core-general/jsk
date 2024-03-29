package sk.web.infogatherer;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.mvn.model.ApiMethodModel;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.web.WebMethodType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class WebMethodInfo {
    Method method;
    String fullMethodPath;
    WebMethodType type;
    ParameterNameAndType returnValue;
    List<ParameterNameAndType> paramAndTypes;
    ApiMethodModel precompiledModel;

    @Data
    @AllArgsConstructor
    public static class ParameterNameAndType {
        String name;
        TypeWrap type;
        boolean merging;

        public String getTypeName() {
            return O.ofNull(type).flatMap($ -> O.ofNull($.getType())).flatMap($ -> O.ofNull($.getTypeName())).orElse("NONE");
        }
    }

    public String postmanize() {
        return Cc.stream(fullMethodPath.split("/"))
                .filter($ -> St.isNotNullOrEmpty($.trim()))
                .map($ -> "\"" + $.trim() + "\"").collect(Collectors.joining(","));
    }
}
