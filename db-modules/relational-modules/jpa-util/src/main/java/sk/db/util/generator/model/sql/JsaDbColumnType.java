package sk.db.util.generator.model.sql;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.db.relational.types.*;
import sk.utils.functional.O;
import sk.utils.statics.Fu;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;

@Getter
@AllArgsConstructor
public enum JsaDbColumnType {
    UUID("UUID", UUID.class, of(UTUuidIdToUuid.class)),

    TEXT("TEXT", String.class, of(UTTextIdToVarchar.class)),

    INT1("INTEGER", Integer.class, of(UTIntIdToInt.class)),
    INT2("INT", Integer.class, of(UTIntIdToInt.class)),
    INT3("INT4", Integer.class, of(UTIntIdToInt.class)),

    BIGINT1("BIGINT", Long.class, of(UTLongIdToBigInt.class)),
    BIGINT2("INT8", Long.class, of(UTLongIdToBigInt.class)),

    BOOLEAN1("BOOLEAN", Boolean.class, empty()),
    BOOLEAN2("BOOL", Boolean.class, empty()),

    TIMESTAMP("TIMESTAMP", ZonedDateTime.class, of(UTZdtToTimestamp.class)),
    JSON("JSONB", Object.class, of(UTObjectToJsonb.class));

    String sqlType;
    Class javaType;
    O<Class> defaultConverterToNonStandardObject;

    public static JsaDbColumnType parse(String type) {
        return Arrays.stream(values())
                .filter($ -> Fu.equal($.getSqlType().trim(), type.trim()))
                .findFirst()
                .get();
    }
}
