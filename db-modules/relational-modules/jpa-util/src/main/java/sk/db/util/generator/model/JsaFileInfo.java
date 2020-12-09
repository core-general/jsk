package sk.db.util.generator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JsaFileInfo {
    String filePrefix;
    String contents;
}
