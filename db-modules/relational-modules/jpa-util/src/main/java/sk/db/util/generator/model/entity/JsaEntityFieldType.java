package sk.db.util.generator.model.entity;

import sk.db.util.generator.model.sql.JsaDbColumnType;
import sk.db.util.generator.model.sql.JsaTableColumn;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;

import static sk.db.util.generator.model.sql.metainfo.JsaMetaType.RELATION_IN_FILE;
import static sk.db.util.generator.model.sql.metainfo.JsaMetaType.RELATION_OUTSIDE;

public enum JsaEntityFieldType {
    ID,
    RELATION_IN,
    RELATION_OUT,
    ENUM,
    ZDT,
    JSONB,
    VERSION,
    OTHER;

    public static JsaEntityFieldType fromTableColumn(JsaTableColumn field) {
        if (field.isId()) {
            return ID;
        } else if (field.getMeta().map($ ->
                $.containsKey(RELATION_IN_FILE)).orElse(false)) {
            return RELATION_IN;
        } else if (field.getMeta().map($ ->
                $.containsKey(RELATION_OUTSIDE)).orElse(false)) {
            return RELATION_OUT;
        } else if (field.getMeta().map($ ->
                $.containsKey(JsaMetaType.ENUM)).orElse(false)) {
            return ENUM;
        } else if (field.getType() == JsaDbColumnType.TIMESTAMP) {
            return ZDT;
        } else if (field.getType() == JsaDbColumnType.JSON) {
            return JSONB;
        } else if (field.getColumnName().equalsIgnoreCase("version")) {
            return VERSION;
        }

        return OTHER;
    }
}
