package jsk.progen.model.enums;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
