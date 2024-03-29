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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.aws.AwsUtilityHelper;
import sk.aws.dynamo.extensions.CreatedAndUpdatedAtExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;

import java.util.List;

@Configuration
public abstract class DynBeanConfig extends DynConfigurator {
    /* must be */
    @Bean /* in inheritor */
    public abstract DynProperties DynProperties();

    @Bean
    public DynClient DynClient(DynProperties properties, AwsUtilityHelper helper,
            CreatedAndUpdatedAtExtension createUpdateExtension,
            List<DynamoDbEnhancedClientExtension> plugins) {
        return new DynClient(properties, this, helper, createUpdateExtension, plugins);
    }

    @Override
    @Bean
    public CreatedAndUpdatedAtExtension CreatedAndUpdatedAtExtension() {
        return super.CreatedAndUpdatedAtExtension();
    }
}
