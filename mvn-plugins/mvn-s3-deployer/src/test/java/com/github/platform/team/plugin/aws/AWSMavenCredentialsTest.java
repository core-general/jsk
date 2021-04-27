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

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AWSMavenCredentialsTest {

    @Test
    public void getAWSAccessKeyId() {
        // GIVEN
        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName("username");
        AWSMavenCredentials awsMavenCredentials = new AWSMavenCredentials(authenticationInfo);

        // WHEN
        String actual = awsMavenCredentials.getAWSAccessKeyId();

        // THEN
        assertThat(actual, equalTo(authenticationInfo.getUserName()));
    }

    @Test
    public void getAWSSecretKey() {
        // GIVEN
        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setPassword("password");
        AWSMavenCredentials awsMavenCredentials = new AWSMavenCredentials(authenticationInfo);

        // WHEN
        String actual = awsMavenCredentials.getAWSSecretKey();

        // THEN
        assertThat(actual, equalTo(authenticationInfo.getPassword()));
    }
}
