package jsk.progen.model.creation;

import jsk.progen.model.JpgModel;
import jsk.progen.model.JpgPackage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import sk.utils.files.PathWithBase;

@EqualsAndHashCode(callSuper = true)
@Getter
public class JpgTaskContextFolder extends JpgTaskContextBase {
    private final PathWithBase currentFolder;

    public JpgTaskContextFolder(PathWithBase basePath, PathWithBase currentFolder, JpgPackage pckg, JpgModel model) {
        super(basePath, pckg);
        this.currentFolder = currentFolder;
    }
}

