package sk.spring;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import sk.utils.functional.O;
import sk.utils.logging.JskLogging;
import sk.utils.statics.Io;
import sk.utils.statics.St;

@SuppressWarnings("WeakerAccess")
public class SpringApp<K extends SpringAppEntryPoint> {

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static <K extends SpringAppEntryPoint> SpringApp<K> createSimple(K processorWillBeAutowired, Class<?> configClass) {
        return new SpringApp<>(O.empty(), O.empty(), processorWillBeAutowired, configClass);
    }

    /**
     * @param welcomeTextFolderOrFile https://patorjk.com/software/taag/#p=display&f=Graffiti&t=Type%20Something%20
     */
    public static <K extends SpringAppEntryPoint> SpringApp<K> createWithWelcomeAndLogAndInit(
            String welcomeTextFolderOrFile,
            JskLogging logger,
            K processorWillBeAutowired,
            Class<?> configClass) {
        return new SpringApp<>(O.ofNull(welcomeTextFolderOrFile), O.ofNull(logger),
                processorWillBeAutowired, configClass);
    }


    public static SpringApp<SpringAppEntryPoint> createWithWelcomeAndLog(
            String welcomeTextFolderOrFile,
            JskLogging logger,
            Class<?> configClass) {
        return new SpringApp<>(O.ofNull(welcomeTextFolderOrFile), O.ofNull(logger), () -> {}, configClass);
    }

    public synchronized void closeContext() {
        if (context != null) {context.close();}
    }

    private final AnnotationConfigApplicationContext context;

    protected SpringApp(O<String> welcomeTextFolderOrFileOrText, O<JskLogging> oLogger, K processor,
            Class<?> configClass) {
        String profileName = O.ofNull(System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME))
                .map($ -> {
                    if ($.contains(",")) {
                        throw new IllegalArgumentException("Jsk allows single active profile only!");
                    }
                    return $;
                }).orElse(/*AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME=*/"default");

        oLogger.ifPresent(logger -> logger.prepare("-" + profileName));

        context = new AnnotationConfigApplicationContext();
        context.setAllowBeanDefinitionOverriding(false);
        context.register(configClass);
        context.refresh();
        context.getBeanFactory().autowireBean(processor);

        prepareWelcomeText(profileName, "Preparation finished");

        processor.run();

        welcomeTextFolderOrFileOrText
                .or(() -> O.of("Processor finished"))
                .ifPresent(welcomeTextFolderOrPath -> prepareWelcomeText(profileName, welcomeTextFolderOrPath));
    }

    protected void prepareWelcomeText(String profile, String welcomeTextFolderOrFileOrText) {
        final String welcomeText = "\n" + Io.getResource(welcomeTextFolderOrFileOrText)
                .or(() -> Io.getResource(St.endWith(welcomeTextFolderOrFileOrText, "/") + "welcome-" + profile + ".txt"))
                .orElse(welcomeTextFolderOrFileOrText);
        try {
            Logger log = LoggerFactory.getLogger(this.getClass());
            log.info(welcomeText);
        } catch (Exception e) {
            System.out.println(welcomeText);
        }
    }
}
