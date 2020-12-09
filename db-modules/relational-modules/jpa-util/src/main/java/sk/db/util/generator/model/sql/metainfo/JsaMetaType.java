package sk.db.util.generator.model.sql.metainfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.statics.Fu;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum JsaMetaType {
    RELATION_IN_FILE("@relationHere", 1/*tableName in this sql file*/),
    RELATION_OUTSIDE("@relationOutside", 2/*id class, jpa class*/),
    JSON("@jsonb", 1/*jsonb class*/),
    ENUM("@enum", 1/*enum class*/);

    String alias;
    int paramCount;

    public static JsaMetaType parse(String s) {
        return Arrays.stream(values())
                .filter($ -> Fu.equal($.getAlias(), "@" + s.trim()))
                .findAny()
                .get();
    }
}
