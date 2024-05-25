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

import lombok.AllArgsConstructor;
import sk.utils.functional.O;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

import java.util.Iterator;

@AllArgsConstructor
public class DynTable<T> {
    private static final Iterator<Page<?>> EMPTY_ITERATOR = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Page<?> next() {
            return null;
        }
    };

    O<DynamoDbTable<T>> table;

    public boolean isReady() {
        return table.map($ -> $.describeTable().table().tableStatus() == TableStatus.ACTIVE).orElse(false);
    }

    public String tableName() {
        return table.map($ -> $.tableName()).orElse("NOT_CONNECTED");
    }

    public void createTable() {
        table.ifPresent($ -> $.createTable());
    }

    public PageIterable<T> query(QueryEnhancedRequest build) {
        return table.map($ -> $.query(build)).orElseGet(() -> () -> (Iterator<Page<T>>) (Object) EMPTY_ITERATOR);
    }

    public void putItem(T item) {
        table.ifPresent($ -> $.putItem(item));
    }

    public void deleteItem(Key key) {
        table.ifPresent($ -> $.deleteItem(key));
    }

    public T consistentGetItem(Key key) {
        return table.map($ -> $.getItem(GetItemEnhancedRequest.builder().key(key).consistentRead(true).build())).orElse(null);
    }
}
