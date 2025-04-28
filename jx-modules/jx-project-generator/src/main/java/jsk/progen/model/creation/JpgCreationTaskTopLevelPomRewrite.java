package jsk.progen.model.creation;

import jsk.progen.model.enums.JpgFileTemplates;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class JpgCreationTaskTopLevelPomRewrite extends JpgCreationTaskBase<JpgTaskContextFolder> {
    JpgFileTemplates<JpgTaskContextFolder> template;

    public JpgCreationTaskTopLevelPomRewrite(JpgTaskContextFolder ctx, JpgFileTemplates<JpgTaskContextFolder> template) {
        super(ctx);
        this.template = template;
    }
}
