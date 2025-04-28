package jsk.progen.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JpgTemplateModel {
    JpgPackage pck;
    JpgServiceDefinition currentService;
    JpgModel model;
}
