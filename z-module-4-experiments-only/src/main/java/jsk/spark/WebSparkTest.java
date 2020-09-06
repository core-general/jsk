package jsk.spark;

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

import jsk.spark.testmodel.SomeClass1;
import jsk.spark.testmodel.SomeClass2;
import jsk.spark.testmodel.SomeClass3;
import jsk.spark.testmodel.SomeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import sk.services.except.IExcept;
import sk.services.profile.IAppProfile;
import sk.services.profile.IAppProfileType;
import sk.services.rand.IRand;
import sk.spring.SpringApp;
import sk.spring.SpringAppEntryPoint;
import sk.spring.config.SpringCoreConfig;
import sk.spring.config.SpringCoreConfigWithProperties;
import sk.spring.services.AppProfileImpl;
import sk.utils.functional.O;
import sk.utils.statics.Io;
import sk.web.exceptions.IWebExcept;
import sk.web.server.WebServerCore;
import sk.web.server.WebServerCoreWithPings;
import sk.web.server.spark.WebJettyEntryPoint;
import sk.web.server.spark.spring.WebSparkCoreConfig;
import web.config.WebCoreConfig;

import javax.inject.Inject;

public class WebSparkTest {
    public static void main(String[] args) {
        final SpringApp<SpringAppEntryPoint> rSpringApp = SpringApp.createWithWelcomeAndLogAndInit(
                "Hello!", "tst_logger", new WebJettyEntryPoint(), Config.class);

        Io.endlessReadFromKeyboard("exit", in -> { });
    }


    @AllArgsConstructor
    public enum BuildType implements IAppProfileType {
        DEFAULT(false, true), DEV(false, false), PROD(true, false);
        @Getter boolean forProductionUsage;
        @Getter boolean forDefaultTesting;
    }


    public static class TestApiImpl implements TestApi1 {
        @Inject IWebExcept webExcept;
        @Inject IExcept except;
        @Inject IRand rnd;


        //@Override
        //public int testInt(int a) {
        //    return except.throwByCode("TestApiImpl1");
        //}
        //
        //@Override
        //public byte[] testIntPost(int a) {
        //    System.out.println(a);
        //    return "TestApiImpl1 ".getBytes(StandardCharsets.UTF_8);
        //}
        //
        //@Override
        //public String testIntPostMultiForce(byte[] body) {
        //    return O.of(body).map($ -> "TestApiImpl1" + new String($, StandardCharsets.UTF_8)).orElse("???");
        //}

        @Override
        public String a(String abc) {
            return abc;
        }

        @Override
        public SomeClass1 b(SomeClass2 abc, SomeEnum x) {
            return new SomeClass1(SomeEnum.THREE, "str", O.of(1), abc, new SomeClass3(), 5);
        }
    }

    @Configuration
    @Import(value = {
            SpringCoreConfig.class,
            WebCoreConfig.class,
            WebSparkCoreConfig.class,
            SpringCoreConfigWithProperties.class
    })
    public static class Config {
        @Bean
        public WebServerCore<TestApi1> WebServerCore(TestApiImpl impl) {
            return new WebServerCoreWithPings<>(TestApi1.class, impl);
        }

        @Bean
        public TestApiImpl TestApiImpl() {
            return new TestApiImpl();
        }

        @Bean
        IAppProfile<BuildType> IAppProfile() {
            return new AppProfileImpl<>(BuildType.class);
        }
    }
}
