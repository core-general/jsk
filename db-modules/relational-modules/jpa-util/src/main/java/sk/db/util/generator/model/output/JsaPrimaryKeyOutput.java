package sk.db.util.generator.model.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.db.util.generator.model.entity.JsaEntityField;

@AllArgsConstructor
@Getter
public class JsaPrimaryKeyOutput {
    String packageName;
    JsaEntityField key;
}
