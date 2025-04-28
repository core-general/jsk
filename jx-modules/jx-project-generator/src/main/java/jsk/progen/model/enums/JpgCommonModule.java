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

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.ifaces.Identifiable;

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
//todo     List<JpgFileTemplates> templates;

    @Override
    public String getId() {
        return suffix;
    }
}
