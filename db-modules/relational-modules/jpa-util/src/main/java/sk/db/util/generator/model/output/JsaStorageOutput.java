package sk.db.util.generator.model.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.db.util.generator.model.entity.JsaEntityModel;

import java.util.List;

@AllArgsConstructor
@Getter
public class JsaStorageOutput {
    String packageName;
    String prefix;
    List<JsaEntityModel> entites;
}
