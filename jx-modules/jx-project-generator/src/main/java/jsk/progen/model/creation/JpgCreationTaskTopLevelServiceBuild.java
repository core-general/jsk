package jsk.progen.model.creation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class JpgCreationTaskTopLevelServiceBuild extends JpgCreationTaskBase<JpgTaskContextService> {
    public JpgCreationTaskTopLevelServiceBuild(JpgTaskContextService ctx) {
        super(ctx);
    }
}
