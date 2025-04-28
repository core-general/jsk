package jsk.progen.model.enums;

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
            L2_BUILD_SERVICE_POM,


            )),
    PUBLIC("public", Cc.s(COM_MODEL)),
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
    List<JpgFileTemplates> templates;

    JpgServiceModule(String suffix, Set<JpgCommonModule> dependentCommonModules, List<JpgFileTemplates> templates) {
        this.dependentCommonModules = dependentCommonModules;
        this.suffix = suffix;
        this.templates = templates;
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
