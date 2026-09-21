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
import sk.services.CoreServicesRaw;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpDeadlineTest {
    @Test void totalDeadlineCancelsStalledAndContinuouslyArrivingBodies() throws Exception {
        var http = CoreServicesRaw.services().http();
        for (boolean trickle : new boolean[]{false, true}) {
            try (var peer = new IncompleteResponse(trickle)) {
                var response = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> http.get(peer.url())
                        .timeout(Duration.ofSeconds(10)).totalTimeout(Duration.ofMillis(500)).goResponse());
                assertEquals(0, peer.started.getCount());
                assertTrue(response.isRight());
                assertInstanceOf(HttpTimeoutException.class, response.right());
                if (trickle) assertTrue(peer.writes.get() > 1);
                peer.release.countDown();
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
                peer.release.countDown();
                assertTrue(peer.disconnected.await(3, TimeUnit.SECONDS));
            } finally { caller.interrupt(); caller.join(3000); }
        }
    }

    private static final class IncompleteResponse implements AutoCloseable {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch disconnected = new CountDownLatch(1);
        final AtomicInteger writes = new AtomicInteger();
        final ExecutorService workers = Executors.newCachedThreadPool();
        final HttpServer server;

        IncompleteResponse(boolean trickle) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(workers);
            server.createContext("/", exchange -> {
                try {
                    exchange.sendResponseHeaders(200, 0);
                    var output = exchange.getResponseBody();
                    output.write('{');
                    output.flush();
                    writes.incrementAndGet();
                    started.countDown();
                    while (!release.await(20, TimeUnit.MILLISECONDS)) {
                        if (trickle) {
                            output.write(' ');
                            output.flush();
                            writes.incrementAndGet();
                        }
                    }
                    long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
                    while (System.nanoTime() < deadline) {
                        output.write(new byte[8192]);
                        output.flush();
                        Thread.sleep(10);
                    }
                } catch (IOException e) { disconnected.countDown(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            server.start();
        }

        String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/"; }
        @Override public void close() {
            release.countDown();
            server.stop(0);
            workers.shutdownNow();
        }
    }
}
