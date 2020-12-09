package sk.db.util.generator.model.sql.metainfo;

import lombok.Getter;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;

import java.util.List;

@Getter
public class JsaMetaInfo {
    JsaMetaType type;
    String table;
    String field;
    List<String> params;

    public JsaMetaInfo(JsaMetaType type, String table, String field, List<String> params) {
        this.type = type;
        this.params = params;

        if (params.size() != type.getParamCount()) {
            Ex.thRow("params.size()!=type.getParamCount() -> " + type + " " + Cc.join(params));
        }
    }
}
