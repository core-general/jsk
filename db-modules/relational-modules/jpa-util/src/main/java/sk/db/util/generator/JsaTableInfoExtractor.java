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

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import sk.db.util.generator.model.sql.JsaTableInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class JsaTableInfoExtractor {
    public static List<JsaTableInfo> extractTableInfo(String fullSqlCode) {
        String sqlCode = Cc.stream(fullSqlCode.split("\n"))
                .map(String::trim)
                .filter($ -> !$.startsWith("--"))
                .collect(joining("\n"));

        Map<X2<String, String>, Map<JsaMetaType, JsaMetaInfo>> tableFieldsToMetaInfos =
                Cc.stream(fullSqlCode.split("\n"))
                        .map(String::trim)
                        .filter($ -> $.startsWith("--  @") || $.startsWith("-- @") || $.startsWith("--@"))
                        .map($ -> St.subLF($, "@").trim())
                        .filter($ -> !$.startsWith("--"))
                        .map($ -> $.split("\\s"))
                        .map($ -> {
                            final X2<X2<String, String>, JsaMetaInfo> x = X.x(X.x($[1].trim(), $[2].trim()),
                                    new JsaMetaInfo(JsaMetaType.parse($[0]), $[1], $[2], Cc.l($).subList(3, $.length)));
                            return x;
                        })
                        .collect(groupingBy($ -> $.i1, mapping($ -> $.i2, toMap(x -> x.getType(), x -> x))));


        List<JsaTableInfo> tables = Cc.stream(sqlCode.split(";"))
                .map(String::trim)
                .filter(sqlStatement -> !sqlStatement.startsWith("--")
                        && sqlStatement.toUpperCase().contains("CREATE TABLE"))
                .map(sql -> {
                    return (CreateTable) Ex.toRuntime(() -> CCJSqlParserUtil.parse(sql));
                })
                .map(table -> new JsaTableInfo(table, tableFieldsToMetaInfos))
                .collect(toList());

        return tables.stream().collect(Cc.toL());
    }

}
