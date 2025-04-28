package jsk.progen.model.enums;

import jsk.progen.model.creation.JpgTaskContextBase;
import jsk.progen.model.creation.JpgTaskContextFolder;
import jsk.progen.model.creation.JpgTaskContextService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.files.PathWithBase;
import sk.utils.functional.F1;
import sk.utils.functional.O;

import static sk.utils.files.PathWithBase.pwb;

@AllArgsConstructor
@Getter
public class JpgFileTemplates<T extends JpgTaskContextBase> {
    public static JpgFileTemplates<JpgTaskContextFolder>
            L0_TOP_BUILD_POM = new JpgFileTemplates<>(O.empty(), "0_intermidiate_pom.pom.ftl");

    public static JpgFileTemplates<JpgTaskContextService>
            L1_TOP_BUILD_SERVICE_POM = new JpgFileTemplates<>(
            O.of(t -> pwb("builds/%s-top-build/pom.xml".formatted(t.c()))), "1_top_level_service_build_pom.pom.ftl");

    public static JpgFileTemplates<JpgTaskContextService>
            L2_BUILD_SERVICE_POM = new JpgFileTemplates<>(
            O.of(t -> pwb("modules/%s/%s-build/pom.xml".formatted(t.c(), t.c()))), "2_service_build_pom.pom.ftl");

    public static JpgFileTemplates<JpgTaskContextService>
            L2_ServiceBuildRootConfig = new JpgFileTemplates<>(
            O.of(t -> pwb("modules/%s/%s-build/src/main/java/%s/%s/build/SynBuildRootConfig.java".formatted(t.c(), t.c(),
                    t.getPckg().getPathNoSlash(), t.c()))), "2_service_build_root_config.java.ftl");

    O<F1<T, PathWithBase>> pathRelativeToRoot;
    String templateFile;
}
