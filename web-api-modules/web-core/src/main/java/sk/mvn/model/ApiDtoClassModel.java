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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sk.utils.functional.O;
import sk.utils.statics.St;

import java.util.List;


@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiDtoClassModel {
    private String name;
    private boolean isEnum;
    private List<ApiFieldOrParameterModel> fields;
    private O<String> parentType = O.empty();
    private O<String> comment = O.empty();

    public String getParentTypeOrNull() {
        return parentType.orElse(null);
    }

    public String getCommentOrNull() {
        return comment.map($ -> $.replace("\n\n", "*\n⭐")).filter(St::isNotNullOrEmpty).orElse(null);
    }
}
