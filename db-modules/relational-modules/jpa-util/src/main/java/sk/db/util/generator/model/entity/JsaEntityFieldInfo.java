package sk.db.util.generator.model.entity;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import sk.db.util.generator.model.sql.JsaDbColumnType;
import sk.db.util.generator.model.sql.JsaTableColumn;
import sk.db.util.generator.model.sql.JsaTableInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;

import static sk.db.util.generator.model.entity.JsaEntityFieldType.*;
import static sk.db.util.generator.model.entity.JsaEntityRelationType.*;
import static sk.db.util.generator.model.sql.metainfo.JsaMetaType.RELATION_IN_FILE;
import static sk.db.util.generator.model.sql.metainfo.JsaMetaType.RELATION_OUTSIDE;

public record JsaEntityFieldInfo(JsaEntityFieldType type, JsaEntityRelationType relation) {
    public static JsaEntityFieldInfo fromTableColumn(JsaTableInfo table, JsaTableColumn field) {
        JsaEntityFieldType type = OTHER;
        if (table.isMultiColumnPrimaryKey() && field.isId()) {
            type = COMPOSITE_ID;
        } else if (field.isId()) {
            type = ID;
        } else if (field.getMeta().map($ ->
                $.containsKey(JsaMetaType.ENUM)).orElse(false)) {
            type = ENUM;
        } else if (field.getMeta().map($ ->
                $.containsKey(JsaMetaType.PG_ENUM)).orElse(false)) {
            type = PG_ENUM;
        } else if (field.getType() == JsaDbColumnType.TIMESTAMP) {
            type = ZDT;
        } else if (field.getType() == JsaDbColumnType.JSON) {
            type = JSONB;
        } else if (field.getColumnName().equalsIgnoreCase("version")) {
            type = VERSION;
        }

        JsaEntityRelationType relation = NONE;
        if (field.getMeta().map($ ->
                $.containsKey(RELATION_IN_FILE)).orElse(false)) {
            relation = RELATION_IN;
        } else if (field.getMeta().map($ ->
                $.containsKey(RELATION_OUTSIDE)).orElse(false)) {
            relation = RELATION_OUT;
        }

        return new JsaEntityFieldInfo(type, relation);
    }
}
