package sk.aws.dynamo;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import sk.aws.AwsUtilityHelper;
import sk.aws.dynamo.extensions.CreatedAndUpdatedAtExtension;
import sk.utils.functional.O;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

import java.util.List;

@RequiredArgsConstructor
public class DynClient {
    final DynProperties properties;
    final DynConfigurator configurator;
    final AwsUtilityHelper helper;
    final CreatedAndUpdatedAtExtension createUpdateExtension;
    final List<DynamoDbEnhancedClientExtension> plugins;

    O<DynamoDbClient> dynaLowLvl;
    O<DynamoDbEnhancedClient> dynaHighLvl;

    @PostConstruct
    DynClient init() {
        dynaLowLvl = O.of(configurator.DynamoDbClient(properties, helper));
        dynaHighLvl = O.of(configurator.DynamoDbEnhancedClient(dynaLowLvl.get(), createUpdateExtension, plugins));

        try {
            dynaLowLvl.map(DynamoDbClient::describeLimits);
        } catch (Exception e) {
            if (properties.isDynamoDisableOk()) {
                dynaLowLvl = O.empty();
                dynaHighLvl = O.empty();
            } else {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public void deleteTable(String name) {
        dynaLowLvl.ifPresent($ -> $.deleteTable(DeleteTableRequest.builder().tableName(name).build()));
    }

    public <T> DynTable<T> table(String name, TableSchema<T> schema) {
        return dynaHighLvl.map($ -> new DynTable<>(O.of($.table(name, schema)))).orElseGet(() -> new DynTable<>(O.empty()));
    }
}
