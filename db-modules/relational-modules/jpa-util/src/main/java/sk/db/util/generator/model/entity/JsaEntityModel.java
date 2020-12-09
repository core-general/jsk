package sk.db.util.generator.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class JsaEntityModel {
    String table;
    String cls;
    String interfce;
    String simple;
    String schema;
    List<JsaEntityField> fields;


    public boolean hasCreatedAt() {
        return fields.stream().anyMatch(field -> field.getFieldName().equalsIgnoreCase("createdAt"));
    }

    public boolean hasUpdatedAt() {
        return fields.stream().anyMatch(field -> field.getFieldName().equalsIgnoreCase("updatedAt"));
    }

    public List<JsaEntityField> getFieldForFactory() {
        return fields.stream().
                filter(field -> field.getCategory() != JsaEntityFieldType.VERSION
                        && !field.getFieldName().equalsIgnoreCase("createdAt")
                        && !field.getFieldName().equalsIgnoreCase("updatedAt"))
                .collect(Collectors.toList());
    }

    public JsaEntityField getIdField() {
        return getFields().stream()
                .filter(field -> field.getCategory() == JsaEntityFieldType.ID).findFirst()
                .orElseThrow(() -> new RuntimeException(("Primary key not found for: " + table)));

    }
}
