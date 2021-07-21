package sk.web.server.spark;

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

import lombok.extern.log4j.Log4j2;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import sk.services.shutdown.AppStopListener;
import sk.utils.statics.Ex;
import sk.web.server.params.WebServerParams;
import sk.web.server.spark.context.WebJettyContextConsumer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Log4j2
public class WebJettyServerStarter implements AppStopListener {
    WebServerParams params;
    List<WebJettyContextConsumer> contextConsumers;

    private volatile Server jetty;

    public WebJettyServerStarter(WebServerParams params, List<WebJettyContextConsumer> contextConsumers) {
        this.params = params;
        this.contextConsumers = contextConsumers;
    }

    public synchronized void run() {
        if (jetty != null) {
            throw new RuntimeException("Server already started");
        }
        System.setProperty("org.eclipse.jetty.server.Request.maxFormKeys", "250");
        System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize", "" + params.getFormLimit());

        jetty = new Server(params.getPort());
        for (Connector connector : jetty.getConnectors()) {
            params.getIdleTimeout().ifPresent(((AbstractConnector) connector)::setIdleTimeout);
            for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
                if (connectionFactory instanceof HttpConnectionFactory) {
                    ((HttpConnectionFactory) connectionFactory)
                            .getHttpConfiguration()
                            .setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
                }
            }
        }

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setAttribute("org.eclipse.jetty.cookie.sameSiteDefault", HttpCookie.SameSite.STRICT.name());
        contextConsumers.forEach($ -> $.accept(context));
        context.setErrorHandler(new ErrorProcessor());
        jetty.setHandler(context);
        try {
            jetty.start();
        } catch (Exception e) {
            Ex.thRow(e);
        }

        context.getServletHandler().getServlets()[0].getRegistration().setMultipartConfig(
                new MultipartConfigElement("/tmp/srv-mp", params.getFormLimit(), params.getFormLimit(), 0));

        HttpGenerator.setJettyVersion(params.getServerNameHeader());
    }

    @Override
    public long waitBeforeStopMs() {
        return params.getShutdownWait().orElse(100L);
    }

    @Override
    public synchronized void onStop() {
        try {
            jetty.stop();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static class ErrorProcessor extends ErrorPageErrorHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
            try {
                response.getWriter().append("JSK " + response.getStatus() + " HTTP error for " + target);
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }
}
