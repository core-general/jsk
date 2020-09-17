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

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.ZonedDateTime;

import static sk.aws.dynamo.extensions.CreatedAndUpdatedAtExtension.CREATED_AT;
import static sk.aws.dynamo.extensions.CreatedAndUpdatedAtExtension.UPDATED_AT;

@EqualsAndHashCode(callSuper = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class DynKVItem implements DynEntityWithCreatedAndUpdated {
    @Getter(onMethod_ = {@DynamoDbPartitionKey})
    String key1;
    @Getter(onMethod_ = {@DynamoDbSortKey})
    String key2;

    @Getter(onMethod_ = {@DynamoDbAttribute("v")})
    String value;

    @Getter(onMethod_ = {@DynamoDbAttribute("rv")})
    byte[] rawValue;

    @Getter(onMethod_ = {@DynamoDbAttribute("ld")})
    Long lockDate;

    @Getter(onMethod_ = {@DynamoDbAttribute(CREATED_AT)})
    ZonedDateTime createdAt;

    @Getter(onMethod_ = {@DynamoDbAttribute(UPDATED_AT)})
    ZonedDateTime updatedAt;

    @Getter(onMethod_ = {@DynamoDbAttribute("ttl")})
    Long ttl;

    @Getter(onMethod_ = {@DynamoDbVersionAttribute})
    Long version;
}
