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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.junit.jupiter.api.Test;
import sk.utils.functional.O;
import sk.web.server.params.WebServerParams;
import sk.web.server.params.WebServerParams.WebStaticFiles;
import sk.web.server.spark.context.WebJettyContextConsumer;
import spark.Service;
import spark.servlet.SparkApplication;
import spark.servlet.SparkFilter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebJettyServerStarterTest {
    @Test
    void servesSparkRouteWithConfiguredServerHeader() throws Exception {
        int port = availablePort();
        WebJettyServerStarter starter = new WebJettyServerStarter(params(port), List.of(sparkFilter()));

        try {
            starter.run();

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/spark-3-smoke"))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals("spark-3-ok", response.body());
            assertEquals("JSK-Test", response.headers().firstValue("Server").orElseThrow());
        } finally {
            starter.onStop();
        }
    }

    private static WebJettyContextConsumer sparkFilter() {
        return context -> context.addFilter(new FilterHolder(new SparkFilter() {
                    @Override
                    protected SparkApplication[] getApplications(FilterConfig filterConfig) throws ServletException {
                        return new SparkApplication[]{() -> Service.ignite()
                                .get("/spark-3-smoke", (request, response) -> "spark-3-ok")};
                    }
                }),
                "/*",
                EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));
    }

    private static WebServerParams params(int port) {
        return new WebServerParams() {
            @Override
            public String getServerNameHeader() {
                return "JSK-Test";
            }

            @Override
            public int getPort() {
                return port;
            }

            @Override
            public O<Long> getShutdownWait() {
                return O.empty();
            }

            @Override
            public long getFormLimit() {
                return 1_000_000;
            }

            @Override
            public O<Long> getIdleTimeout() {
                return O.empty();
            }

            @Override
            public O<WebStaticFiles> getStaticFilesLocation() {
                return O.empty();
            }

            @Override
            public O<Integer> getTokenTimeoutSec() {
                return O.empty();
            }

            @Override
            public boolean isUseCookiesForToken() {
                return false;
            }
        };
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
