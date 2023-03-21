package jsk.vaadin;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinMVCWebAppInitializer;
import org.atmosphere.cpr.ApplicationConfig;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;
import sk.utils.statics.Re;
import sk.utils.statics.St;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.ClassUtils.getShortNameAsProperty;

public abstract class BaseWebAppInit extends VaadinMVCWebAppInitializer {
    protected abstract Class<?> getRootConfigClass();

    protected SpringServlet initServletClass(AnnotationConfigWebApplicationContext spring, boolean rootMapping) {
        return new SpringServlet(spring, rootMapping);
    }

    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(servletContext);
        registerConfiguration(context);
        servletContext.addListener(new ContextLoaderListener(context));

        context.refresh();

        Environment env = context.getBean(Environment.class);
        String mapping = RootMappedCondition.getUrlMapping(env);
        if (mapping == null) {
            mapping = "/*";
        }

        boolean rootMapping = RootMappedCondition.isRootMapping(mapping);


        final SpringServlet servlet = initServletClass(context, rootMapping); /*MY*/
        ServletRegistration.Dynamic registration = servletContext.addServlet(getShortNameAsProperty(servlet.getClass()), servlet);

        Map<String, String> initParameters = new HashMap<>();
        String pushRegistrationPath;
        if (rootMapping) {
            ServletRegistration.Dynamic dispatcherRegistration =
                    servletContext.addServlet("dispatcher", new DispatcherServlet(context));
            dispatcherRegistration.addMapping(new String[]{"/*"});
            mapping = "/vaadinServlet/*";
            pushRegistrationPath = "";
            dispatcherRegistration.setAsyncSupported(true);
        } else {
            pushRegistrationPath = mapping.replace("/*", "");
        }

        registration.addMapping(new String[]{mapping});
        /*
         * Tell Atmosphere which servlet to use for the push endpoint. Servlet
         * mappings are returned as a Set from at least Tomcat so even if
         * Atmosphere always picks the first, it might end up using /VAADIN/*
         * and websockets will fail.
         */
        initParameters.put(ApplicationConfig.JSR356_MAPPING_PATH, pushRegistrationPath);
        initParameters.put(ApplicationConfig.JSR356_PATH_MAPPING_LENGTH, "0");

        registration.setInitParameters(initParameters);

        registration.setAsyncSupported(true);        /*MY*/
        registration.setInitParameter("pushmode", "automatic");/*MY*/
        registration.setInitParameter("closeIdleSessions", "true");/*MY*/

        prepareLogger("bi_logger"); /*MY*/
    }

    protected boolean prepareLogger(String loggerFolderOrFile) {
        String profile = O.ofNull(System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME))
                .map($ -> {
                    if ($.contains(",")) {
                        throw new IllegalArgumentException("Jsk allows single active profile only!");
                    }
                    return $;
                }).orElse(/*AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME=*/"default");

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

    static String makeContextRelative(String url) {
        // / -> context://
        // foo -> context://foo
        // /foo -> context://foo
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        return "context://" + url;
    }

    @Override
    protected final Collection<Class<?>> getConfigurationClasses() {
        return List.of(getRootConfigClass());
    }
}
