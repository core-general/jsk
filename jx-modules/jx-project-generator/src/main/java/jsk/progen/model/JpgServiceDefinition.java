package jsk.progen.model;

import jsk.progen.model.enums.JpgServiceModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JpgServiceDefinition {
    boolean hasRootBuild;
    Set<JpgServiceModule> usedCommonModules;
}
