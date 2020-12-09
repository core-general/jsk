package sk.db.util.generator.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.statics.St;

@AllArgsConstructor
@Getter
public class JsaEntityField {
    String fieldName;
    String columnName;
    String mainType;
    String idType;
    String relatedType;
    String converterType;
    boolean nullable;
    JsaEntityFieldType category;

    public String capName() {
        return St.capFirst(fieldName);
    }
}
