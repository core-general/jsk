package sk.db.util.generator.model.sql;

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
import net.sf.jsqlparser.statement.create.table.CreateTable;
import sk.db.util.generator.model.sql.metainfo.JsaMetaInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class JsaTableInfo {
    String tableName;
    List<JsaTableColumn> fields;

    public JsaTableInfo(CreateTable table,
            Map<X2<String, String>, Map<JsaMetaType, JsaMetaInfo>> tableFieldsToMetaInfos) {
        this.tableName = table.getTable().getName();
        fields = table.getColumnDefinitions().stream()
                .map($ -> {
                    final String fieldName = $.getColumnName().toLowerCase();
                    return new JsaTableColumn(fieldName,
                            JsaDbColumnType.parse($.getColDataType().getDataType().toUpperCase()),
                            $.getColumnSpecs() != null
                                    && $.getColumnSpecs().stream().map(x -> x.toUpperCase()).collect(Cc.toS())
                                    .containsAll(Cc.l("PRIMARY", "KEY")),
                            !($.getColumnSpecs() != null
                                    && $.getColumnSpecs().stream().map(x -> x.toUpperCase()).collect(Cc.toS())
                                    .containsAll(Cc.l("NOT", "NULL"))),
                            O.ofNull(tableFieldsToMetaInfos.get(X.x(tableName, fieldName)))
                    );
                }).collect(Cc.toL());
    }
}
