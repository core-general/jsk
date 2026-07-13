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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MultiPartCompliance;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import sk.services.shutdown.AppStopListener;
import sk.utils.statics.Ex;
import sk.web.server.params.WebServerParams;
import sk.web.server.spark.context.WebJettyContextConsumer;

import jakarta.servlet.MultipartConfigElement;
import java.util.List;

@Slf4j
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

        jetty = new Server(
                params.getPort()
        );
        for (Connector connector : jetty.getConnectors()) {
            params.getIdleTimeout().ifPresent(((AbstractConnector) connector)::setIdleTimeout);
            for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
                if (connectionFactory instanceof HttpConnectionFactory) {
                    HttpConfiguration httpConfiguration = ((HttpConnectionFactory) connectionFactory)
                            .getHttpConfiguration();
                    httpConfiguration.setMultiPartCompliance(MultiPartCompliance.RFC7578);
                    httpConfiguration.setSendServerVersion(false);
                    httpConfiguration.addCustomizer((request, responseHeaders) -> {
                        responseHeaders.put(HttpHeader.SERVER, params.getServerNameHeader());
                        return request;
                    });
                }
            }
        }

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setMaxFormKeys(250);
        context.setMaxFormContentSize(Math.toIntExact(params.getFormLimit()));
        context.setAttribute("org.eclipse.jetty.cookie.sameSiteDefault", HttpCookie.SameSite.STRICT.name());
        contextConsumers.forEach($ -> $.accept(context));
        context.setErrorHandler(new ErrorProcessor());
        jetty.setHandler(context);
        try {
            jetty.start();
        } catch (Exception e) {
            Ex.thRow(e);
        }

        log.info("Jetty started on port:" + params.getPort());

        context.getServletHandler().getServlets()[0].getRegistration().setMultipartConfig(
                new MultipartConfigElement("/tmp/srv-mp", params.getFormLimit(), params.getFormLimit(), 0));
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
            log.error("", e);
        }
    }

    public static class ErrorProcessor extends ErrorPageErrorHandler {
        @Override
        protected void generateResponse(Request request, Response response, int code, String message,
                Throwable cause, Callback callback) {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain; charset=UTF-8");
            Content.Sink.write(response, true,
                    "JSK " + code + " HTTP error for " + Request.getPathInContext(request), callback);
        }
    }
}
