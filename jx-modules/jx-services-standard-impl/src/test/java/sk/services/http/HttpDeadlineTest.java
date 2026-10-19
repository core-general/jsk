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

import org.junit.jupiter.api.Test;
import sk.services.CoreServicesRaw;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpDeadlineTest {
    @Test void totalDeadlineCancelsStalledAndContinuouslyArrivingBodies() throws Exception {
        var http = CoreServicesRaw.services().http();
        for (boolean trickle : new boolean[]{false, true}) {
            try (var peer = new IncompleteResponse(trickle)) {
                // Establish client/server readiness before testing the short body deadline.
                assertTrue(http.get(peer.url() + "warm").timeout(Duration.ofSeconds(10))
                        .totalTimeout(Duration.ofSeconds(5)).goResponse().isLeft());
                var response = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> http.get(peer.url())
                        .timeout(Duration.ofSeconds(10)).totalTimeout(Duration.ofMillis(150)).goResponse());
                assertEquals(0, peer.started.getCount());
                assertTrue(response.isRight());
                assertInstanceOf(HttpTimeoutException.class, response.right());
                if (trickle) assertTrue(peer.writes.get() > 1);
                assertTrue(peer.disconnected.await(3, TimeUnit.SECONDS), "HTTP exchange was not cancelled");
            }
        }
    }

    @Test void interruptionCancelsTheExchangeAndPreservesTheCallerFlag() throws Exception {
        var http = CoreServicesRaw.services().http();
        try (var peer = new IncompleteResponse(false)) {
            AtomicBoolean interrupted = new AtomicBoolean();
            AtomicReference<Exception> failure = new AtomicReference<>();
            Thread caller = Thread.ofPlatform().start(() -> {
                var response = http.get(peer.url()).totalTimeout(Duration.ofSeconds(30)).goResponse();
                interrupted.set(Thread.currentThread().isInterrupted());
                if (response.isRight()) failure.set(response.right());
            });
            try {
                assertTrue(peer.started.await(3, TimeUnit.SECONDS));
                caller.interrupt();
                caller.join(3000);
                assertFalse(caller.isAlive());
                assertTrue(interrupted.get());
                assertInstanceOf(IOException.class, failure.get());
                assertTrue(peer.disconnected.await(3, TimeUnit.SECONDS), "Interrupted HTTP exchange was not cancelled");
            } finally { caller.interrupt(); caller.join(3000); }
        }
    }

    private static final class IncompleteResponse implements AutoCloseable {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch disconnected = new CountDownLatch(1);
        final AtomicInteger writes = new AtomicInteger();
        final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
        final ServerSocket server;

        IncompleteResponse(boolean trickle) throws IOException {
            server = new ServerSocket();
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            workers.submit(() -> {
                boolean completed = false;
                while (!completed) try (var socket = server.accept()) {
                    socket.setTcpNoDelay(true);
                    var input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    String request = input.readLine();
                    for (String line; (line = input.readLine()) != null && !line.isEmpty();) {}
                    var output = socket.getOutputStream();
                    if (request != null && request.startsWith("GET /warm ")) {
                        output.write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                        output.flush(); continue;
                    }
                    output.write(("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n1\r\n{\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    output.flush();
                    writes.incrementAndGet();
                    started.countDown();
                    if (trickle) {
                        workers.submit(() -> {
                            try {
                                while (!disconnected.await(20, TimeUnit.MILLISECONDS)) {
                                    output.write("1\r\n \r\n".getBytes(StandardCharsets.US_ASCII));
                                    output.flush();
                                    writes.incrementAndGet();
                                }
                            } catch (IOException e) {
                                throw new java.io.UncheckedIOException(e);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }
                    try {
                        if (input.read() != -1) throw new IOException("Unexpected data after the request headers");
                    } catch (SocketException resetByClient) {
                    }
                    disconnected.countDown(); completed = true;
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }

        String url() { return "http://127.0.0.1:" + server.getLocalPort() + "/"; }
        @Override public void close() throws Exception {
            server.close();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS), "HTTP test peer did not stop");
        }
    }
}
