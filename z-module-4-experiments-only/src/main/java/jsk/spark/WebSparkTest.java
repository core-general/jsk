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
import sk.aws.dynamo.DynBeanConfigWithKvStore;
import sk.aws.dynamo.DynProperties;
import sk.aws.spring.AwsBeanConfig;
import sk.services.except.IExcept;
import sk.services.idempotence.IIdempotenceParameters;
import sk.services.idempotence.IIdempotenceProviderUnlimitedKV;
import sk.services.json.IJson;
import sk.services.profile.IAppProfile;
import sk.services.profile.IAppProfileType;
import sk.services.rand.IRand;
import sk.spring.SpringApp;
import sk.spring.SpringAppEntryPoint;
import sk.spring.config.SpringCoreConfig;
import sk.spring.config.SpringCoreConfigWithProperties;
import sk.spring.services.AppProfileImpl;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Io;
import sk.web.exceptions.IWebExcept;
import sk.web.server.WebServerCore;
import sk.web.server.WebServerCoreWithPings;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.filters.additional.WebRequestFullInfo;
import sk.web.server.filters.additional.WebUserActionLoggingFilter;
import sk.web.server.filters.additional.WebUserHistoryAdditionalDataProvider;
import sk.web.server.filters.additional.WebUserHistoryProvider;
import sk.web.server.params.WebApiInfoParams;
import sk.web.server.params.WebIdempotenceParams;
import sk.web.server.params.WebUserActionLoggerParams;
import sk.web.server.spark.WebJettyEntryPoint;
import sk.web.server.spark.spring.WebSparkCoreConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import web.config.WebCoreConfig;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static sk.utils.functional.O.of;
import static sk.utils.functional.O.ofNull;

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
        @Inject IJson json;
        @Inject IRand rnd;
        @Inject WebUserHistoryProvider historyProvider;

        @Override
        public String a(String abc) {
            //final String join = Cc.join("\n===================================================================\n\n",
            //        historyProvider.getRenderedUserHistory(abc,
            //                of(ZonedDateTime.parse("2020-09-17T15:10:00.000Z", ISO_DATE_TIME)),
            //                of(ZonedDateTime.parse("2021-09-17T16:10:00.000Z", ISO_DATE_TIME)),
            //                10,
            //                false),
            //        x -> St.addTabsLeft(O.ofNull(x.i3())
            //                .map($ -> Ti.yyyyMMddHHmmssSSS.format($) + "\n").orElse("") + json.to(x.i1(), false) + x.i2(), 2));
            //System.out.println(join);

            final List<WebRequestFullInfo> fullUserHistory = historyProvider.getFullUserHistory(abc,
                    of(ZonedDateTime.parse("2020-09-17T15:10:00.000Z", ISO_DATE_TIME)),
                    of(ZonedDateTime.parse("2021-09-17T16:10:00.000Z", ISO_DATE_TIME)),
                    100,
                    false);
            System.out.println(json.to(fullUserHistory, true));

            return abc;
        }

        @Override
        public SomeClass1 b(SomeClass2 abc, SomeEnum x) {
            return new SomeClass1(SomeEnum.THREE, "str", O.of(1), abc, new SomeClass3(), 5);
        }

        @Override
        public Map<String, Integer> testWebUserToken(Map<String, String> a) {
            return a.entrySet().stream().collect(Cc.toM($ -> $.getKey(), $ -> $.getValue().length()));
        }
    }

    @Configuration
    @Import(value = {
            AwsBeanConfig.class,
            UserLoggingConfig.class,
            SpringCoreConfig.class,
            WebCoreConfig.class,
            WebSparkCoreConfig.class,
            SpringCoreConfigWithProperties.class
    })
    public static class Config {
        @Bean
        public WebServerCore<TestApi1> WebServerCore(TestApiImpl impl, WebUserActionLoggingFilter actionLogger) {
            return new WebServerCoreWithPings<TestApi1>(TestApi1.class, impl) {
                @Override
                protected O<List<WebServerFilter>> getAdditionalFilters(O<Method> methodOrAll) {
                    return of(Cc.l(actionLogger));
                }
            };
        }

        @Bean
        IIdempotenceProviderUnlimitedKV IIdempotenceProviderSingleNode() {
            return new IIdempotenceProviderUnlimitedKV();
        }

        @Bean
        IIdempotenceParameters IIdempotenceParameters() {
            return () -> true;
        }

        @Bean
        WebIdempotenceParams WebIdempotenceParams() {
            return new WebIdempotenceParams() {
                @Override
                public Duration getLockDuration() {
                    return Duration.ofMinutes(5);
                }

                @Override
                public Duration getCacheDuration() {
                    return Duration.ofMinutes(10);
                }
            };
        }

        @Bean
        public TestApiImpl TestApiImpl() {
            return new TestApiImpl();
        }

        @Bean
        IAppProfile<BuildType> IAppProfile() {
            return new AppProfileImpl<>(BuildType.class);
        }

        @Bean
        WebApiInfoParams WebApiInfoParams() {
            return new WebApiInfoParams() {
                @Override
                public String getBasicAuthLogin() {
                    return "abc";
                }

                @Override
                public String getBasicAuthPass() {
                    return "def";
                }
            };
        }

        @Bean
        WebUserHistoryAdditionalDataProvider WebUserHistoryAdditionalDataProvider() {
            return new WebUserHistoryAdditionalDataProvider() {
                @Override
                public O<Object> provideAdditionalData(WebServerFilterContext<?> ctx) {
                    return ofNull("WPOWOWO");
                }

                @Override
                public String getName() {
                    return "abc";
                }
            };
        }

        @Bean
        WebUserHistoryAdditionalDataProvider WebUserHistoryAdditionalDataProvider1() {
            return new WebUserHistoryAdditionalDataProvider() {
                @Override
                public O<Object> provideAdditionalData(WebServerFilterContext<?> ctx) {
                    return of(Cc.m("a", Cc.m("b", "c", "d", "e"), "b", "wowowow"));
                }

                @Override
                public String getName() {
                    return "def";
                }
            };
        }
    }

    @Configuration
    public static class UserLoggingConfig extends DynBeanConfigWithKvStore {
        @Bean
        public WebUserActionLoggerParams WebUserActionLoggerParams() {
            //todo all parameters should be taken from DB as before
            return new WebUserActionLoggerParams() {
                @Override
                public boolean isOn() {
                    return true;
                }

                @Override
                public Duration getTtl() {
                    return Duration.ofDays(30);
                }
            };
        }

        @Bean
        public WebUserActionLoggingFilter WebUserActionLoggingFilter() {
            return new WebUserActionLoggingFilter() {
                @Override
                public O<String> getUserIdByToken(String userToken) {
                    try {
                        return O.of(userToken);
                    } catch (Exception e) {
                        return O.empty();
                    }
                }
            };
        }

        @Override
        @Bean
        public DynProperties DynProperties() {
            return new DynProperties() {
                @Override
                public String getTablePrefix() {
                    return "DEF_";
                }

                @Override
                public OneOf<URI, Region> getAddress() {
                    return OneOf.left(URI.create("http://localhost:8000"));
                }

                @Override
                public AwsCredentials getCredentials() {
                    return AwsBasicCredentials.create("abc", "bcd");
                }
            };
        }
    }
}
