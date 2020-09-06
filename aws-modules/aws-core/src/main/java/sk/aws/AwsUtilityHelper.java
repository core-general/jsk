package sk.aws;

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

import sk.utils.functional.Gett;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.regions.Region;

public class AwsUtilityHelper {
    public <Y extends AwsClientBuilder<Y, X> & AwsSyncClientBuilder<Y, X>, X> X createSync(Gett<Y> newClientBuilder,
            AwsProperties properties) {
        Y clientBuilder = newClientBuilder.apply();

        clientBuilder = provideEndpointAndCredentials(clientBuilder, properties);
        clientBuilder = provideJskSyncWebTransport(clientBuilder, properties);

        return clientBuilder.build();
    }

    public <Y extends AwsClientBuilder<Y, X> & AwsAsyncClientBuilder<Y, X>, X> X createAsync(Gett<Y> newClientBuilder,
            AwsProperties properties) {
        Y clientBuilder = newClientBuilder.apply();
        clientBuilder = provideEndpointAndCredentials(clientBuilder, properties);
        return clientBuilder.build();
    }

    protected <Y extends AwsClientBuilder<Y, X>, X> Y provideEndpointAndCredentials(Y clientBuilder, AwsProperties properties) {
        Y finalClientBuilder = clientBuilder;
        clientBuilder = properties.getAddress().collect(
                endpointOverride -> {
                    finalClientBuilder.endpointOverride(endpointOverride);
                    finalClientBuilder.region(Region.US_EAST_1);
                    return finalClientBuilder;
                },
                region -> finalClientBuilder.region(region)
        );

        clientBuilder = clientBuilder.credentialsProvider(properties::getCredentials);

        return clientBuilder;
    }

    protected <Y extends AwsSyncClientBuilder<Y, X>, X> Y provideJskSyncWebTransport(Y clientBuilder, AwsProperties properties) {
        //implement Jsk Web request
        //clientBuilder = clientBuilder.httpClient(new SdkHttpClient() {
        //    @Override
        //    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        //        return null;
        //    }
        //
        //    @Override
        //    public void close() {
        //
        //    }
        //});

        return clientBuilder;
    }
}
