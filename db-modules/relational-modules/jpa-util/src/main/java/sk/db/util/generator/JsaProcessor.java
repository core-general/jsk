package sk.db.util.generator;

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
import sk.db.util.generator.model.entity.*;
import sk.db.util.generator.model.sql.JsaDbColumnType;
import sk.db.util.generator.model.sql.JsaRawSqlInfo;
import sk.db.util.generator.model.sql.JsaTableColumn;
import sk.db.util.generator.model.sql.JsaTableInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.St;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class JsaProcessor {
    public static JsaFullEntityModel process(String prefix, JsaRawSqlInfo fullData) {
        final List<JsaEnumEntityModel> enums = fullData.enums().stream()
                .map($ -> new JsaEnumEntityModel($.getEnumTypeName(), St.snakeToCamelCase($.getEnumTypeName()), $.getMembers()))
                .toList();

        final List<JsaEntityModel> tables = fullData.tables().stream()
                .map($ -> toModel(prefix, $))
                .collect(Cc.toL());

        return new JsaFullEntityModel(enums, tables);
    }

    private static JsaEntityModel toModel(String prefix, JsaTableInfo table) {
        JsaNaming naming = new JsaNaming(prefix, table.getTableName(), 2);

        final String iface = naming.getName(0, true);
        final String idClass = iface + "Id";

        return new JsaEntityModel(
                table.getTableName(),
                naming.getNameJpa(0, true),
                iface,
                naming.getName(1, false),
                naming.getSchema(),
                possibleCompositeField(prefix, table, idClass),
                table.getFields().stream()
                        .map($ -> createField(prefix, table, $, idClass))
                        .collect(Cc.toL())
        );
    }

    private static O<JsaEntityCompositeKey> possibleCompositeField(String prefix, JsaTableInfo table, String idClass) {
        if (table.isMultiColumnPrimaryKey()) {
            final List<JsaEntityField> collect = table.getFields().stream().filter($ -> $.isId())
                    .map($ -> createField(prefix, table, $, idClass))
                    .collect(Collectors.toList());

            final ListIterator<JsaTableColumn> itToDelete = table.getFields().listIterator();
            while (itToDelete.hasNext()) {
                final JsaTableColumn next = itToDelete.next();
                if (collect.stream().anyMatch($ -> $.getColumnName().equalsIgnoreCase(next.getColumnName()))) {
                    itToDelete.remove();
                }
            }
            table.getFields().add(0, new JsaTableColumn(
                    "id", JsaDbColumnType.COMPOSITE_ID, true, false, O.empty(), O.empty()
            ));

            JsaEntityCompositeKey composite = new JsaEntityCompositeKey(
                    idClass, collect
            );

            return O.of(composite);
        } else {
            return O.empty();
        }
    }

    private static JsaEntityField createField(String prefix, JsaTableInfo table, JsaTableColumn column, String idClass) {
        final JsaEntityFieldInfo fieldType = JsaEntityFieldInfo.fromTableColumn(table, column);
        final Class javaType = column.getType().getJavaType();
        String mainType = javaType == byte[].class ? "byte[]" : javaType.getName();
        String idType = null;
        String relationType = null;
        boolean needSeparateIdFile = true;

        if (fieldType.type() == JsaEntityFieldType.ID || fieldType.type() == JsaEntityFieldType.COMPOSITE_ID) {
            idType = mainType;
            mainType = idClass;
        } else if (fieldType.type() == JsaEntityFieldType.ENUM) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.ENUM);
            mainType = meta.getParams().get(0);
        } else if (fieldType.type() == JsaEntityFieldType.PG_ENUM) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.PG_ENUM);
            mainType = meta.getParams().get(0);
        } else if (fieldType.type() == JsaEntityFieldType.JSONB) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.JSON);
            mainType = meta.getParams().get(0);
        }

        if (fieldType.relation() == JsaEntityRelationType.RELATION_IN) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.RELATION_IN_FILE);
            String foreignTable = meta.getParams().get(0);

            JsaNaming naming = new JsaNaming(prefix, foreignTable, 1);

            mainType = naming.getName(0, true) + "Id";
            relationType = naming.getNameJpa(0, true);
        } else if (fieldType.relation() == JsaEntityRelationType.RELATION_OUT) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.RELATION_OUTSIDE);
            mainType = meta.getParams().get(0);
            relationType = meta.getParams().get(1);
        } else if (fieldType.type() == JsaEntityFieldType.COMPOSITE_ID && column.getType() != JsaDbColumnType.COMPOSITE_ID) {
            mainType = idType;
            needSeparateIdFile = false;
        }

        return new JsaEntityField(
                new JsaNaming("", column.getColumnName(), 1).getName(0, false),
                column.getColumnName(),
                mainType,
                idType,
                relationType,
                column.getType().getDefaultConverterToNonStandardObject().map($ -> $.getName()).orElse(null),
                column.isNullable(),
                fieldType.type(),
                fieldType.relation(),
                needSeparateIdFile
        );
    }

    @Getter
    private static class JsaNaming {
        private List<String> nameSequence;
        private List<String> nameSequence4Jpa;

        public JsaNaming(String prefix, String tableName, int minSize) {
            nameSequence = Cc.l(tableName.split("_")).stream()
                    .filter(St::isNotNullOrEmpty)
                    .collect(Collectors.toList());
            if (St.isNotNullOrEmpty(prefix)) {
                nameSequence.add(0, St.capFirst(prefix));
            }
            if (nameSequence.size() < minSize) {
                Ex.thRow(nameSequence.size() + " < " + minSize + " for " + tableName);
            }

            nameSequence4Jpa = new ArrayList<>(nameSequence);
            nameSequence4Jpa.set(nameSequence4Jpa.size() - 1, nameSequence4Jpa.get(nameSequence4Jpa.size() - 1) + "Jpa");
        }

        public String getName(int skipItems, boolean firstCapped) {
            return getName(skipItems, firstCapped, nameSequence);
        }

        public String getNameJpa(int skipItems, boolean firstCapped) {
            return getName(skipItems, firstCapped, nameSequence4Jpa);
        }

        private String getName(int skipItems, boolean firstCapped, List<String> selector) {
            if (skipItems > 0) {
                selector = selector.subList(skipItems, selector.size());
            }
            return Cc.join("", Cc.mapEachWithIndex(selector, (a, i) -> (i == 0 && firstCapped || i != 0) ? St.capFirst(a) : a));
        }

        private String getSchema() {
            return St.capFirst(nameSequence.get(0));
        }
    }
}
