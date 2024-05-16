package sk.test;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import sk.services.profile.IAppProfileType;
import sk.spring.SpringApp;
import sk.spring.SpringAppEntryPoint;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class JskSpringTest<T> implements SpringAppEntryPoint {
    public SpringApp<SpringAppEntryPoint> context;
    private AutoCloseable closeableTest;

    @BeforeEach
    public void initSpringTest() {
        context = SpringApp.createSimple(this, getRootConfig(), getProfile());
        closeableTest = MockitoAnnotations.openMocks(this);
        Mockito.validateMockitoUsage();
    }


    @AfterEach
    public void destroyMockitoTest() throws Exception {
        closeableTest.close();
    }

    public abstract Class<T> getRootConfig();

    public abstract IAppProfileType getProfile();

    @Override
    public void run() {}
}
