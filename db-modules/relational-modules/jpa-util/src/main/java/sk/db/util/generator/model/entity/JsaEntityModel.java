package sk.db.util.generator.model.entity;

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
import lombok.Getter;
import sk.utils.functional.O;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class JsaEntityModel {
    String table;
    String cls;
    String iFace;
    String simple;
    String schema;
    O<JsaEntityCompositeKey> compositeId;
    List<JsaEntityField> fields;


    public boolean hasCreatedAt() {
        return fields.stream().anyMatch(field -> field.getFieldName().equalsIgnoreCase("createdAt"));
    }

    public boolean hasUpdatedAt() {
        return fields.stream().anyMatch(field -> field.getFieldName().equalsIgnoreCase("updatedAt"));
    }

    public List<JsaEntityField> getFieldForFactory() {
        return fields.stream().
                filter(field -> field.getCategory() != JsaEntityFieldType.VERSION
                        && !field.getFieldName().equalsIgnoreCase("createdAt")
                        && !field.getFieldName().equalsIgnoreCase("updatedAt"))
                .collect(Collectors.toList());
    }

    public JsaEntityField getIdField() {
        return getFields().stream()
                .filter(field -> field.getCategory() == JsaEntityFieldType.ID ||
                        field.getCategory() == JsaEntityFieldType.COMPOSITE_ID).findFirst()
                .orElseThrow(() -> new RuntimeException(("Primary key not found for: " + table)));

    }

    public boolean isComposite() {
        return getCompositeId().isPresent();
    }
}
