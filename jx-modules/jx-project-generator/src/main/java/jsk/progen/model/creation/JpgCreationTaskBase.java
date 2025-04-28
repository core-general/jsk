package jsk.progen.model.creation;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public sealed abstract class JpgCreationTaskBase<T extends JpgTaskContextBase>
        permits JpgCreationTaskCommonModule, JpgCreationTaskNormalModule, JpgCreationTaskTopLevelPomRewrite,
                JpgCreationTaskTopLevelServiceBuild {
    private T ctx;

    public T c() {
        return ctx;
    }
}
