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

import sk.db.util.generator.model.sql.JsaDbColumnType;
import sk.db.util.generator.model.sql.JsaTableColumn;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;

import static sk.db.util.generator.model.sql.metainfo.JsaMetaType.RELATION_IN_FILE;
import static sk.db.util.generator.model.sql.metainfo.JsaMetaType.RELATION_OUTSIDE;

public enum JsaEntityFieldType {
    ID,
    RELATION_IN,
    RELATION_OUT,
    ENUM,
    PG_ENUM,
    ZDT,
    JSONB,
    VERSION,
    OTHER;

    public static JsaEntityFieldType fromTableColumn(JsaTableColumn field) {
        if (field.isId()) {
            return ID;
        } else if (field.getMeta().map($ ->
                $.containsKey(RELATION_IN_FILE)).orElse(false)) {
            return RELATION_IN;
        } else if (field.getMeta().map($ ->
                $.containsKey(RELATION_OUTSIDE)).orElse(false)) {
            return RELATION_OUT;
        } else if (field.getMeta().map($ ->
                $.containsKey(JsaMetaType.ENUM)).orElse(false)) {
            return ENUM;
        } else if (field.getMeta().map($ ->
                $.containsKey(JsaMetaType.PG_ENUM)).orElse(false)) {
            return PG_ENUM;
        } else if (field.getType() == JsaDbColumnType.TIMESTAMP) {
            return ZDT;
        } else if (field.getType() == JsaDbColumnType.JSON) {
            return JSONB;
        } else if (field.getColumnName().equalsIgnoreCase("version")) {
            return VERSION;
        }

        return OTHER;
    }
}
