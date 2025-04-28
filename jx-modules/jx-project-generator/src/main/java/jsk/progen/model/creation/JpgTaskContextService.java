package jsk.progen.model.creation;

import jsk.progen.model.JpgPackage;
import jsk.progen.model.JpgServiceId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import sk.utils.files.PathWithBase;

@EqualsAndHashCode(callSuper = true)
@Getter
public class JpgTaskContextService extends JpgTaskContextBase {
    private final JpgServiceId service;

    public JpgTaskContextService(PathWithBase basePath, JpgPackage pckg, JpgServiceId service) {
        super(basePath, pckg);
        this.service = service;
    }

    public JpgServiceId c() {
        return service;
    }
}
