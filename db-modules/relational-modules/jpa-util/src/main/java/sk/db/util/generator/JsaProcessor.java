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
import sk.db.util.generator.model.entity.JsaEntityField;
import sk.db.util.generator.model.entity.JsaEntityFieldType;
import sk.db.util.generator.model.entity.JsaEntityModel;
import sk.db.util.generator.model.sql.JsaTableColumn;
import sk.db.util.generator.model.sql.JsaTableInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.St;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsaProcessor {
    public static List<JsaEntityModel> process(List<JsaTableInfo> tables) {
        return tables.stream()
                .map($ -> toModel($))
                .collect(Cc.toL());
    }

    private static JsaEntityModel toModel(JsaTableInfo table) {
        JsaNaming naming = new JsaNaming(table.getTableName(), 2);

        final String iface = naming.getName(0, true);
        final String idClass = iface + "Id";

        return new JsaEntityModel(
                table.getTableName(),
                naming.getNameJpa(0, true),
                iface,
                naming.getName(1, false),
                naming.getSchema(),
                table.getFields().stream()
                        .map($ -> createField($, idClass))
                        .collect(Cc.toL())
        );
    }

    private static JsaEntityField createField(JsaTableColumn column, String idClass) {
        final JsaEntityFieldType fieldType = JsaEntityFieldType.fromTableColumn(column);
        final Class javaType = column.getType().getJavaType();
        String mainType = javaType == byte[].class ? "byte[]" : javaType.getName();
        String idType = null;
        String relationType = null;

        if (fieldType == JsaEntityFieldType.ID) {
            idType = mainType;
            mainType = idClass;
        } else if (fieldType == JsaEntityFieldType.RELATION_IN) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.RELATION_IN_FILE);
            String foreignTable = meta.getParams().get(0);

            JsaNaming naming = new JsaNaming(foreignTable, 2);

            mainType = naming.getName(0, true) + "Id";
            relationType = naming.getNameJpa(0, true);
        } else if (fieldType == JsaEntityFieldType.RELATION_OUT) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.RELATION_OUTSIDE);
            mainType = meta.getParams().get(0);
            relationType = meta.getParams().get(1);
        }
        if (fieldType == JsaEntityFieldType.ENUM) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.ENUM);
            mainType = meta.getParams().get(0);
        }
        if (fieldType == JsaEntityFieldType.JSONB) {
            JsaMetaInfo meta = column.getMeta().get().get(JsaMetaType.JSON);
            mainType = meta.getParams().get(0);
        }

        return new JsaEntityField(
                new JsaNaming(column.getColumnName(), 1).getName(0, false),
                column.getColumnName(),
                mainType,
                idType,
                relationType,
                column.getType().getDefaultConverterToNonStandardObject().map($ -> $.getName()).orElse(null),
                column.isNullable(),
                fieldType
        );
    }

    @Getter
    private static class JsaNaming {
        private List<String> nameSequence;
        private List<String> nameSequence4Jpa;

        public JsaNaming(String tableName, int minSize) {
            nameSequence = Cc.l(tableName.split("_")).stream()
                    .filter(St::isNotNullOrEmpty)
                    .collect(Collectors.toList());
            if (nameSequence.size() < minSize) {
                Ex.thRow(nameSequence.size() + " < 2 for " + tableName);
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
