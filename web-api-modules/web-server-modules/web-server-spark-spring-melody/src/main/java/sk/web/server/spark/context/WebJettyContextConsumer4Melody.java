package sk.web.server.spark.context;

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

import net.bull.javamelody.MonitoringFilter;
import net.bull.javamelody.Parameter;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.core.annotation.Order;
import sk.web.server.spark.melodycollector.WebMelodyParams;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import java.util.EnumSet;

@Order(0)
public class WebJettyContextConsumer4Melody implements WebJettyContextConsumer {
    @Inject WebMelodyParams params;

    @Override
    public void accept(ServletContextHandler context) {
        if (params.isMelodyEnabled()) {
            final FilterHolder filterHolder = new FilterHolder(new MonitoringFilter());

            filterHolder.setInitParameter(Parameter.SAMPLING_SECONDS.getCode(), "3");
            filterHolder.setInitParameter(Parameter.STORAGE_DIRECTORY.getCode(), "/tmp/jsk/melody");
            filterHolder.setInitParameter(Parameter.AUTHORIZED_USERS.getCode(), params.getLogin() + ":" + params.getPass());
            filterHolder
                    .setInitParameter(Parameter.MONITORING_PATH.getCode(), params.getMelodyPathSuffix().orElse("/monitoring"));

            context.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));
        }
    }
}
