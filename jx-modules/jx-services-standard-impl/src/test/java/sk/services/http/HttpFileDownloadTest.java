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

import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import sk.services.CoreServicesRaw;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;

class HttpFileDownloadTest {
    @TempDir Path temporary;
    private HttpServer server;
    private ExecutorService workers;
    private final IHttp http = CoreServicesRaw.services().http();

    @BeforeEach void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        workers = Executors.newCachedThreadPool();
        server.setExecutor(workers);
        server.start();
    }

    @AfterEach void stop() {
        server.stop(0);
        workers.shutdownNow();
    }

    private String url(String path) { return "http://127.0.0.1:" + server.getAddress().getPort() + path; }

    @Test void streamsPostThroughRedirectsAndDecodesGzipWithoutChangingByteMethods() throws Exception {
        byte[] content = "café ".repeat(10000).getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> header = new AtomicReference<>();
        server.createContext("/post", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            header.set(exchange.getRequestHeaders().getFirst("X-Test"));
            exchange.getResponseHeaders().set("Location", "/gzip");
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        });
        server.createContext("/gzip", exchange -> {
            exchange.getRequestBody().transferTo(OutputStream.nullOutputStream());
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.getResponseHeaders().set("X-Result", "yes");
            exchange.sendResponseHeaders(200, 0);
            try (var output = new GZIPOutputStream(exchange.getResponseBody())) { output.write(content); }
            finally { exchange.close(); }
        });
        Path target = temporary.resolve("body");
        var response = http.postBody(url("/post")).body("payload").headers(Map.of("X-Test", "present"))
                .goToFile(target, content.length).left();
        assertEquals("payload", body.get());
        assertEquals("present", header.get());
        assertEquals("yes", response.getHeader("X-Result").get());
        assertEquals(0, response.getAsBytes().length);
        assertArrayEquals(content, Files.readAllBytes(target));
        assertArrayEquals(content, http.get(url("/gzip")).goBytesAndThrow());
        try (var children = Files.list(temporary)) { assertEquals(List.of(target), children.toList()); }
    }

    @Test void boundsErrorPreviewsAndPreservesGatewayStatusWrappers() throws Exception {
        for (int code : new int[]{403, 503}) server.createContext("/" + code, exchange -> {
            exchange.sendResponseHeaders(code, 0);
            try (var output = exchange.getResponseBody()) {
                for (int n = 0; n < 256; n++) output.write(new byte[1024]);
            } catch (IOException ignored) {} finally { exchange.close(); }
        });
        var denied = http.get(url("/403")).goToFile(temporary.resolve("denied"), 1024).left();
        assertEquals(403, denied.code());
        assertEquals(65536, denied.getAsBytes().length);
        var gateway = http.get(url("/503")).goToFile(temporary.resolve("gateway"), 1024).right();
        var retry = assertInstanceOf(HttpImpl.RetryException.class, gateway);
        assertEquals(503, retry.getCode());
        assertEquals(65536, retry.getResp().getAsBytes().length);
        assertEmpty();
    }

    @Test void limitsBothWireAndDecodedBodiesAndReleasesPartialFiles() throws Exception {
        server.createContext("/wire", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) { output.write(new byte[100_000]); }
            catch (IOException ignored) {} finally { exchange.close(); }
        });
        server.createContext("/gzip", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, 0);
            try (var output = new GZIPOutputStream(exchange.getResponseBody())) { output.write(new byte[100_000]); }
            finally { exchange.close(); }
        });
        for (String path : new String[]{"/wire", "/gzip"}) {
            var response = http.get(url(path)).goToFile(temporary.resolve("limited"), 1024);
            assertInstanceOf(HttpResponseLimitException.class, response.right());
            assertEmpty();
        }
        IllegalStateException full = new IllegalStateException("Disk budget exhausted");
        var response = http.get(url("/wire")).goToFile(temporary.resolve("quota"), 200_000, n -> { throw full; });
        assertSame(full, response.right());
        assertEmpty();
    }

    @Test void deadlinesIncludeStalledAndTricklingFileBodies() throws Exception {
        for (boolean trickle : new boolean[]{false, true}) {
            String path = "/timeout-" + trickle;
            server.createContext(path, exchange -> {
                exchange.sendResponseHeaders(200, 0);
                try (var output = exchange.getResponseBody()) {
                    output.write(1); output.flush();
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(20);
                        if (trickle) { output.write(1); output.flush(); }
                    }
                } catch (IOException ignored) {} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            var result = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> http.get(url(path))
                    .totalTimeout(Duration.ofMillis(200)).goToFile(temporary.resolve("timeout"), 10000));
            assertInstanceOf(HttpTimeoutException.class, result.right());
            assertEmpty();
        }
    }

    @Test void interruptionTruncationAndExistingDestinationsDoNotLeavePartialFiles() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        server.createContext("/wait", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                output.write(1); output.flush(); started.countDown();
                Thread.sleep(10000);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread caller = Thread.ofPlatform().start(() -> {
            assertTrue(http.get(url("/wait")).goToFile(temporary.resolve("interrupted"), 10000).isRight());
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        assertTrue(started.await(3, TimeUnit.SECONDS));
        caller.interrupt(); caller.join(3000);
        assertFalse(caller.isAlive()); assertTrue(interrupted.get()); assertEmpty();
        server.createContext("/short", exchange -> {
            exchange.sendResponseHeaders(200, 10000);
            exchange.getResponseBody().write(1);
            exchange.close();
        });
        assertTrue(http.get(url("/short")).totalTimeout(Duration.ofSeconds(2))
                .goToFile(temporary.resolve("short"), 10000).isRight());
        assertEmpty();
        Path existing = temporary.resolve("existing");
        Files.writeString(existing, "keep");
        assertInstanceOf(FileAlreadyExistsException.class,
                http.get(url("/short")).goToFile(existing, 10000).right());
        assertEquals("keep", Files.readString(existing));
    }

    @Test void downloadsAnArchiveLargerThanTheChildJvmHeap() throws Exception {
        int size = 128 * 1024 * 1024;
        byte[] block = new byte[8192];
        CRC32 crc = new CRC32();
        for (int n = 0; n < size; n += block.length) crc.update(block);
        server.createContext("/large", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var zip = new ZipOutputStream(exchange.getResponseBody())) {
                ZipEntry padding = new ZipEntry("padding");
                padding.setMethod(ZipEntry.STORED); padding.setSize(size); padding.setCrc(crc.getValue());
                zip.putNextEntry(padding);
                for (int n = 0; n < size; n += block.length) zip.write(block);
                zip.closeEntry(); zip.putNextEntry(new ZipEntry("data/value.txt")); zip.write("42".getBytes()); zip.closeEntry();
            } finally { exchange.close(); }
        });
        Path log = temporary.resolve("probe.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx64m", "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                Probe.class.getName(), url("/large"), temporary.resolve("large.zip").toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(40, TimeUnit.SECONDS), "Streaming probe timed out");
            assertEquals(0, process.exitValue(), () -> {
                try { return Files.readString(log); } catch (IOException e) { return e.toString(); }
            });
        } finally { process.destroyForcibly(); }
    }

    public static class Probe {
        public static void main(String[] args) throws Exception {
            Path file = Path.of(args[1]);
            var response = CoreServicesRaw.services().http().get(args[0]).totalTimeout(Duration.ofSeconds(30))
                    .goToFile(file, 150L * 1024 * 1024);
            if (response.isRight()) throw response.right();
            if (Files.size(file) <= Runtime.getRuntime().maxMemory()) throw new AssertionError("Fixture is too small");
            try (ZipFile zip = new ZipFile(file.toFile()); var input = zip.getInputStream(zip.getEntry("data/value.txt"))) {
                if (!"42".equals(new String(input.readAllBytes(), StandardCharsets.UTF_8))) throw new AssertionError();
            }
        }
    }

    private void assertEmpty() throws IOException {
        try (var paths = Files.list(temporary)) { assertEquals(List.of(), paths.toList()); }
    }
}
