package sk.db.util.generator.model.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.db.util.generator.model.entity.JsaEntityModel;

@AllArgsConstructor
@Getter
public class JsaEntityOutput {
    String packageName;
    String dbSchema;
    JsaEntityModel model;
}
