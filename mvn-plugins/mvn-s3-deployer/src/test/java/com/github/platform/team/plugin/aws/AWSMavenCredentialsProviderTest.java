package com.github.platform.team.plugin.aws;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import com.amazonaws.auth.AWSCredentials;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class AWSMavenCredentialsProviderTest {

    @Test
    public void getCredentialsIfNull() {
        // GIVEN
        AWSMavenCredentialsProvider provider = new AWSMavenCredentialsProvider(null);

        // WHEN
        AWSCredentials actual = provider.getCredentials();

        // THEN
        assertThat(actual, nullValue());
    }

    @Test
    public void getCredentials() {
        // GIVEN
        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName("username");
        authenticationInfo.setPassword("password");
        AWSMavenCredentialsProvider provider = new AWSMavenCredentialsProvider(authenticationInfo);

        // WHEN
        AWSCredentials actual = provider.getCredentials();

        // THEN
        assertThat(actual.getAWSAccessKeyId(), equalTo(authenticationInfo.getUserName()));
        assertThat(actual.getAWSSecretKey(), equalTo(authenticationInfo.getPassword()));
    }
}
