package sk.db.util.generator.model.sql.metainfo;

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
import sk.utils.statics.Fu;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum JsaMetaType {
    RELATION_IN_FILE("@relationHere", 1/*tableName in this sql file*/),
    RELATION_OUTSIDE("@relationOutside", 2/*id class, jpa class*/),
    JSON("@jsonb", 1/*jsonb class*/),
    ENUM("@enum", 1/*enum class*/),
    PG_ENUM("@pg_enum", 1/*pg enum class*/);

    String alias;
    int paramCount;

    public static JsaMetaType parse(String s) {
        return Arrays.stream(values())
                .filter($ -> Fu.equal($.getAlias(), "@" + s.trim()))
                .findAny()
                .get();
    }
}
