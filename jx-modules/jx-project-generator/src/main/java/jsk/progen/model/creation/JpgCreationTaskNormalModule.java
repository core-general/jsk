package jsk.progen.model.creation;

import jsk.progen.model.enums.JpgServiceModule;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class JpgCreationTaskNormalModule extends JpgCreationTaskBase<JpgTaskContextService> {
    JpgServiceModule module;

    public JpgCreationTaskNormalModule(JpgTaskContextService ctx, JpgServiceModule module) {
        super(ctx);
        this.module = module;
    }
}
