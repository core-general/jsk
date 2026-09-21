package sk.services.http;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sk.services.CoreServicesRaw;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HttpFileBodyTest {
    @TempDir Path temporary;

    @Test void sendsAFileBodyWithoutFollowingAnOptOutRedirect() throws Exception {
        Path file = temporary.resolve("payload.zip");
        byte[] chunk = "binary-file-content".repeat(1000).getBytes();
        MessageDigest expected = MessageDigest.getInstance("SHA-256");
        try (var output = Files.newOutputStream(file)) {
            for (int i = 0; i < 100; i++) {
                output.write(chunk);
                expected.update(chunk);
            }
        }
        AtomicInteger redirects = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/upload", exchange -> {
            try {
                MessageDigest actual = MessageDigest.getInstance("SHA-256");
                try (var input = exchange.getRequestBody()) {
                    byte[] buffer = new byte[8192];
                    for (int n; (n = input.read(buffer)) >= 0;) actual.update(buffer, 0, n);
                }
                exchange.getResponseHeaders().set("X-Body-Sha", HexFormat.of().formatHex(actual.digest()));
                exchange.getResponseHeaders().set("Location", "/redirected");
                exchange.sendResponseHeaders(302, -1);
            } catch (Exception error) {
                throw new RuntimeException(error);
            } finally { exchange.close(); }
        });
        server.createContext("/redirected", exchange -> {
            redirects.incrementAndGet();
            exchange.getRequestBody().transferTo(OutputStream.nullOutputStream());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            var http = CoreServicesRaw.services().http();
            var response = http.postBody("http://127.0.0.1:" + server.getAddress().getPort() + "/upload")
                    .bodyFile(file).followRedirects(false).timeout(Duration.ofSeconds(5)).goResponseAndThrow();
            assertEquals(302, response.code());
            assertEquals(HexFormat.of().formatHex(expected.digest()), response.getHeader("X-Body-Sha").orElseThrow());
            assertEquals(0, redirects.get());
        } finally { server.stop(0); }
    }
}
