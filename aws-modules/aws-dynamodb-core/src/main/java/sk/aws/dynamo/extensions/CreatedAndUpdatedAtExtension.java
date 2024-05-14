package sk.aws.dynamo.extensions;

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

import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import sk.services.time.ITime;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class CreatedAndUpdatedAtExtension implements DynamoDbEnhancedClientExtension {
    @Inject ITime times;

    public static final String CREATED_AT = "_cat";
    public static final String UPDATED_AT = "_uat";

    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
        final String now = DateTimeFormatter.ISO_DATE_TIME.format(times.nowZ());
        final Map<String, AttributeValue> transformedItem = new HashMap<>(context.items());
        transformedItem.put(UPDATED_AT, AttributeValue.builder().s(now).build());
        if (transformedItem.get(CREATED_AT) == null) {
            transformedItem.put(CREATED_AT, AttributeValue.builder().s(now).build());
        }
        return WriteModification.builder().transformedItem(transformedItem).build();
    }
}
