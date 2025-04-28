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

import jsk.progen.model.JpgServiceId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.functional.O;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;
import sk.utils.statics.Re;

import java.util.List;
import java.util.Set;

import static jsk.progen.model.enums.JpgCommonModule.*;
import static jsk.progen.model.enums.JpgFileTemplates.L2_BUILD_SERVICE_POM;

@Getter
@AllArgsConstructor
public enum JpgServiceModule implements Identifiable<String> {
    BUILD("build", Cc.s(COM_DB_PG), Cc.l(
            L2_BUILD_SERVICE_POM


            )),
    PUBLIC("public", Cc.s(COM_MODEL),/*todo */ Cc.s()),
    PRIVATE("private", Cc.s(COM_SPRING), Cc.s(PUBLIC)),
    LOGIC("logic", Cc.s(), Cc.s(PRIVATE)),

    DB_DYNAMO("db-dynamo", Cc.s(COM_DB_DYNAMO), Cc.s(PRIVATE)),
    DB_PG("db-pg", Cc.s(COM_DB_PG, COM_MODEL_JPA, COM_MODEL), Cc.s(PRIVATE)),
    DB_S3("db-s3", Cc.s(COM_DB_S3, COM_MODEL), Cc.s(PRIVATE)),
    LOCAL_LANDSCAPE("land", Cc.s(COM_LAND), Cc.s(PRIVATE)),
    SERVER_HTTP("http", Cc.s(COM_HTTP), Cc.s(PRIVATE)),
    LLM("llm", Cc.s(COM_LLM), Cc.s(PRIVATE)),
    TEST("test", Cc.s(COM_TEST), Cc.s(PRIVATE));

    String suffix;
    Set<JpgCommonModule> dependentCommonModules;
    Set<JpgServiceModule> dependentServiceModules = Cc.s();
    //todo List<JpgFileTemplates> templates;

    JpgServiceModule(String suffix, Set<JpgCommonModule> dependentCommonModules, List<JpgFileTemplates> templates) {
        this.dependentCommonModules = dependentCommonModules;
        this.suffix = suffix;
        //todo this.templates = templates;
    }

    public static O<JpgServiceModule> parse(JpgServiceId id, String name) {
        String s = name.replaceFirst(id.toString(), "");
        if (s.startsWith("-")) {
            s = s.substring(1);
        }
        return Re.findInEnum(JpgServiceModule.class, s);
    }

    @Override
    public String getId() {
        return suffix;
    }
}
