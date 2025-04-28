package jsk.progen.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.ifaces.Identifiable;

import java.util.List;

@Getter
@AllArgsConstructor
public enum JpgCommonModule implements Identifiable<String> {
    COM_SPRING("common-spring"),
    COM_MODEL("common-model"),
    COM_MODEL_JPA("common-model-jpa"),
    COM_DB_DYNAMO("common-db-dynamo"),
    COM_DB_PG("common-db-pg"),
    COM_DB_S3("common-db-s3"),
    COM_LAND("common-land"),
    COM_HTTP("common-http"),
    COM_LLM("common-llm"),
    COM_TEST("common-test"),
    ;

    String suffix;
    List<JpgFileTemplates> templates;

    @Override
    public String getId() {
        return suffix;
    }
}
