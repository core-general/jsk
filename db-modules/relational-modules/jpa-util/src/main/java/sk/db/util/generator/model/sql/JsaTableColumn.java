package sk.db.util.generator.model.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.db.util.generator.model.sql.metainfo.JsaMetaInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;
import sk.utils.functional.O;

import java.util.Map;

@Data
@AllArgsConstructor
public class JsaTableColumn {
    String columnName;
    JsaDbColumnType type;
    boolean id;
    boolean nullable;
    O<Map<JsaMetaType, JsaMetaInfo>> meta;
}
