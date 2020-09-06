package sk.aws.dynamo;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import sk.aws.AwsUtilityHelper;
import sk.aws.dynamo.extensions.CreatedAndUpdatedAtExtension;
import sk.utils.statics.Cc;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.ArrayList;
import java.util.List;


public class DynConfigurator {
    public DynamoDbClient DynamoDbClient(DynProperties properties, AwsUtilityHelper helper) {
        return helper.createSync(DynamoDbClient::builder, properties);
    }

    public DynamoDbEnhancedClient DynamoDbEnhancedClient(DynamoDbClient client,
            CreatedAndUpdatedAtExtension createUpdateExtension,
            List<DynamoDbEnhancedClientExtension> plugins) {
        DynamoDbEnhancedClient.Builder builder = DynamoDbEnhancedClient.builder().dynamoDbClient(client);
        builder.extensions(Cc.addAll(new ArrayList<>(plugins), Cc.l(
                VersionedRecordExtension.builder().build(),
                createUpdateExtension
        )));

        return builder.build();
    }


    public CreatedAndUpdatedAtExtension CreatedAndUpdatedAtExtension() {
        return new CreatedAndUpdatedAtExtension();
    }
}
