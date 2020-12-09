package sk.db.util.generator;

import sk.db.util.generator.model.JsaFileInfo;
import sk.db.util.generator.model.entity.JsaEntityModel;
import sk.db.util.generator.model.sql.JsaTableInfo;
import sk.utils.functional.C1;
import sk.utils.statics.Io;
import sk.utils.statics.St;

import java.util.List;

public class JsaMain {
    public void start(String sqlCode, C1<JsaFileInfo> fileProcessor, String schemaName) {
        final List<JsaTableInfo> tables = JsaTableInfoExtractor.extractTableInfo(sqlCode);
        final List<JsaEntityModel> models = JsaProcessor.process(tables);
        JsaExporter.export(fileProcessor, models, schemaName);
    }

    public static void startFileBased(String pathToSqlFile, String fileExportPath, String schemaName) {
        new JsaMain().start(
                Io.sRead(pathToSqlFile).string(),
                info -> Io.reWrite(St.endWith(fileExportPath, "/") + St.notStartWith(info.getFilePrefix(), "/"),
                        w -> w.append(info.getContents())),
                schemaName);
    }

    public static void main(String[] args) {
        startFileBased(args[0], args[1], args[2]);
    }
}
