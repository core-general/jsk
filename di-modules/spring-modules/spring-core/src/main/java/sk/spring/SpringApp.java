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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;
import sk.utils.statics.Re;
import sk.utils.statics.St;

@SuppressWarnings("WeakerAccess")
public class SpringApp<K extends SpringAppEntryPoint> {
    public final static String LOG4J_LOGGER_TYPE = "log4j";

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static <K extends SpringAppEntryPoint> SpringApp<K> createSimple(K processorWillBeAutowired, Class<?> configClass) {
        return new SpringApp<>(O.empty(), O.empty(), processorWillBeAutowired, configClass);
    }

    /**
     * @param welcomeTextFolderOrFile https://patorjk.com/software/taag/#p=display&f=Graffiti&t=Type%20Something%20
     */
    public static <K extends SpringAppEntryPoint> SpringApp<K> createWithWelcomeAndLogAndInit(
            String welcomeTextFolderOrFile,
            String loggerFolderOrFile,
            K processorWillBeAutowired,
            Class<?> configClass) {
        return new SpringApp<>(O.ofNull(welcomeTextFolderOrFile), O.ofNull(loggerFolderOrFile),
                processorWillBeAutowired, configClass);
    }


    public static SpringApp<SpringAppEntryPoint> createWithWelcomeAndLog(
            String welcomeTextFolderOrFile,
            String loggerFolderOrFile,
            Class<?> configClass) {
        return new SpringApp<>(O.ofNull(welcomeTextFolderOrFile), O.ofNull(loggerFolderOrFile), () -> {}, configClass);
    }

    public synchronized void closeContext() {
        if (context != null) { context.close(); }
    }

    private final AnnotationConfigApplicationContext context;

    protected SpringApp(O<String> welcomeTextFolderOrFileOrText, O<String> loggerFolderOrFile, K processor,
            Class<?> configClass) {
        String profileName = O.ofNull(System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME))
                .map($ -> {
                    if ($.contains(",")) {
                        throw new IllegalArgumentException("Jsk allows single active profile only!");
                    }
                    return $;
                }).orElse(/*AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME=*/"default");

        loggerFolderOrFile.ifPresent(loggerFolderOrPath1 -> prepareLogger(profileName, loggerFolderOrPath1));

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
            Logger log = LogManager.getLogger(this.getClass());
            log.info(welcomeText);
        } catch (Exception e) {
            System.out.println(welcomeText);
        }
    }

    protected boolean prepareLogger(String profile, String loggerFolderOrFile) {
        final String log4jLoggerClass = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector";
        O<Class<?>> classIfExist = Re.getClassIfExist(log4jLoggerClass);
        classIfExist.orElseThrow(() -> new IllegalArgumentException("Can't find log4j classes in classpath:" + log4jLoggerClass));
        System.setProperty("Log4jContextSelector", log4jLoggerClass);

        O<String> actualLogFile = O.empty();
        if (loggerFolderOrFile.endsWith(".xml")) {
            actualLogFile = O.of(loggerFolderOrFile);
        } else {
            final String profileBasedFile = St.endWith(loggerFolderOrFile, "/") + "log4j2-" + profile + ".xml";
            actualLogFile = O.of(profileBasedFile);
        }

        actualLogFile.ifPresentOrElse(
                s -> {
                    if (Io.isResourceExists(s)) {
                        System.setProperty("log4j.configurationFile", s);
                    } else {
                        throw new IllegalArgumentException(
                                "Can't find Log4j configuration in file:" + s +
                                        "  with profile:" + profile);
                    }
                },
                () -> {
                    throw new IllegalArgumentException(
                            "Can't find Log4j configuration in either file or folder:" + loggerFolderOrFile +
                                    "  with profile:" + profile);
                }
        );

        //check that default logger does not exist in classpath
        if (!Fu.equal("log4j2.xml", loggerFolderOrFile)) {
            if (Io.isResourceExists("log4j2.xml")) {
                throw new IllegalArgumentException(
                        "Default log4j.xml is in the root of classpath, it could be confused with actual log file:" +
                                actualLogFile.orElse("???"));
            }
        }

        return true;
    }
}
