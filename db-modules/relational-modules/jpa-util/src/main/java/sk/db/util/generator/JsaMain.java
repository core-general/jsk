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

import sk.db.util.generator.model.JsaFileInfo;
import sk.db.util.generator.model.entity.JsaFullEntityModel;
import sk.db.util.generator.model.sql.JsaRawSqlInfo;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.statics.Io;
import sk.utils.statics.St;

public class JsaMain {
    public void start(String sqlCode, C1<JsaFileInfo> fileProcessor, String schemaName, O<String> explicitPrefix) {
        final JsaRawSqlInfo tables = JsaTableInfoExtractor.extractTableInfo(sqlCode);
        final JsaFullEntityModel models = JsaProcessor.process(explicitPrefix.orElse(null), tables);
        JsaExporter.export(fileProcessor, models, schemaName);
    }

    public static void startFileBased(String pathToSqlFile, String fileExportPath, String schemaName, O<String> explicitPrefix) {
        new JsaMain().start(
                Io.sRead(pathToSqlFile).string(),
                info -> Io.reWrite(St.endWith(fileExportPath, "/") + St.notStartWith(info.getFilePrefix(), "/"),
                        w -> w.append(info.getContents())),
                schemaName, explicitPrefix);
    }

    public static void main(String[] args) {
        startFileBased(args[0], args[1], args[2], args.length > 3 ? O.of(args[3]) : O.empty());
    }
}
