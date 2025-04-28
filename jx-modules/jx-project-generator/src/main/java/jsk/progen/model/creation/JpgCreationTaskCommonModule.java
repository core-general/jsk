package jsk.progen.model.creation;

import jsk.progen.model.enums.JpgCommonModule;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class JpgCreationTaskCommonModule extends JpgCreationTaskBase<JpgTaskContextService> {
    JpgCommonModule module;

    public JpgCreationTaskCommonModule(JpgTaskContextService ctx, JpgCommonModule module) {
        super(ctx);
        this.module = module;
    }
}
