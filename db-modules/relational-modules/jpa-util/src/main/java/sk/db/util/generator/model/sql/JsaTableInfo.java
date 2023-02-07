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
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
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
    boolean multiColumnPrimaryKey;

    public JsaTableInfo(CreateTable table,
            Map<X2<String, String>, Map<JsaMetaType, JsaMetaInfo>> tableFieldsToMetaInfos,
            List<JsaRawEnumTypeInfo> enums) {
        this.tableName = table.getTable().getName();
        fields = table.getColumnDefinitions().stream()
                .map(clmn -> {
                    final String fieldName = clmn.getColumnName().toLowerCase();

                    boolean shouldBePgEnum = tableFieldsToMetaInfos.getOrDefault(X.x(tableName, fieldName), Cc.mEmpty())
                            .keySet().stream()
                            .anyMatch(jsaMetaType -> jsaMetaType == JsaMetaType.PG_ENUM);

                    final O<JsaForeignKey> foreignKey = determineForeignKey(table, clmn);
                    Map<JsaMetaType, JsaMetaInfo> meta = tableFieldsToMetaInfos.get(X.x(tableName, fieldName));
                    if (foreignKey.isPresent()) {
                        if (meta == null) {
                            meta = Cc.m();
                        }
                        if (!meta.containsKey(JsaMetaType.RELATION_OUTSIDE) && !meta.containsKey(JsaMetaType.RELATION_IN_FILE)) {
                            meta.put(JsaMetaType.RELATION_IN_FILE,
                                    new JsaMetaInfo(JsaMetaType.RELATION_IN_FILE,
                                            tableName, fieldName,
                                            Cc.l(foreignKey.get().getOtherTable())
                                    ));
                        }
                    }

                    return new JsaTableColumn(fieldName,
                            JsaDbColumnType.parse(clmn.getColDataType().getDataType().toUpperCase(), enums, shouldBePgEnum),
                            determinePrimaryKey(table, clmn),
                            containInColumnSpec(clmn, Cc.l("NOT", "NULL")),
                            foreignKey,
                            O.ofNull(meta)
                    );
                }).collect(Cc.toL());
        multiColumnPrimaryKey = fields.stream().filter($ -> $.isId()).count() > 1;
    }

    private boolean determinePrimaryKey(CreateTable table, ColumnDefinition def) {
        return containInColumnSpec(def, Cc.l("PRIMARY", "KEY")) ||
                O.ofNull(table.getIndexes()).stream()
                        .flatMap($ -> $.stream())
                        .filter($ -> "PRIMARY KEY".equalsIgnoreCase($.getType()))
                        .anyMatch($ -> $.getColumnsNames().contains(def.getColumnName()));
    }

    private O<JsaForeignKey> determineForeignKey(CreateTable table, ColumnDefinition def) {
        return O.of(O.ofNull(table.getIndexes()).stream()
                        .flatMap($ -> $.stream())
                        .filter($ -> "FOREIGN KEY".equalsIgnoreCase($.getType()))
                        .filter($ -> $.getColumnsNames().contains(def.getColumnName()))
                        .map($ -> (ForeignKeyIndex) $)
                        .findAny())
                .map($ -> new JsaForeignKey(
                        $.getTable().getName(),
                        Cc.first($.getReferencedColumnNames()).get()
                ));
    }

    private boolean containInColumnSpec(ColumnDefinition $, List<String> items) {
        return $.getColumnSpecs() != null
                && $.getColumnSpecs().stream().map(x -> x.toUpperCase()).collect(Cc.toS())
                .containsAll(items);
    }
}
