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

import sk.services.time.ITime;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import java.util.Map;

import static sk.utils.statics.Ti.yyyyMMddHHmmssSSS;

public class CreatedAndUpdatedAtExtension implements DynamoDbEnhancedClientExtension {
    @Inject ITime times;

    public static final String CREATED_AT = "_cat";
    public static final String UPDATED_AT = "_uat";

    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
        final String now = yyyyMMddHHmmssSSS.format(times.nowZ());
        final Map<String, AttributeValue> transformedItem = context.items().entrySet().stream().map($ -> {
            if (Fu.equal(CREATED_AT, $.getKey()) && ($.getValue() == null || $.getValue().nul())) {
                $.setValue(AttributeValue.builder().s(now).build());
            } else if (Fu.equal(UPDATED_AT, $.getKey())) {
                $.setValue(AttributeValue.builder().s(now).build());
            }
            return $;
        }).collect(Cc.toMEntry());
        return WriteModification.builder().transformedItem(transformedItem).build();
    }
}
