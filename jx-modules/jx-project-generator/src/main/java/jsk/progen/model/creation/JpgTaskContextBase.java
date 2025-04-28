package jsk.progen.model.creation;

import jsk.progen.model.JpgPackage;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import sk.utils.files.PathWithBase;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class JpgTaskContextBase {
    private final PathWithBase basePath;
    private final JpgPackage pckg;
}

