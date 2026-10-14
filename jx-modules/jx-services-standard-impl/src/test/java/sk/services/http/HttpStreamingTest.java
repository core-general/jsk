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
import sk.services.async.AsyncImpl;
import sk.services.async.IScheduledExecutorService;
import sk.services.async.IScheduledExecutorServiceImpl;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class HttpStreamingTest {
    @Test void everyStatusReachesTheCallbackOnceAndTheBuilderCanBeReused() throws Exception {
        var http=CoreServicesRaw.services().http();
        for (int status:new int[]{200,302,404,503}) {
            var calls=new AtomicInteger();
            var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange -> {
                calls.incrementAndGet(); exchange.getResponseHeaders().add("X-Peer","value");
                byte[] content="response".getBytes(); exchange.sendResponseHeaders(status,content.length);
                try(var body=exchange.getResponseBody()) { body.write(content); }
            });
            server.start();
            try {
                var request=http.get(url(server)).followRedirects(false).tryCount(5);
                var received=new AtomicReference<byte[]>();
                var response=request.goStream(upstream -> {
                    assertEquals(status,upstream.code()); assertEquals("value",upstream.headers().get("x-peer").getFirst());
                    received.set(upstream.body().readAllBytes());
                });
                assertTrue(response.isLeft()); assertEquals(status,response.left().code());
                assertEquals("response",new String(received.get())); assertEquals(1,calls.get());
                assertNull(request.streamConsumer());
                if(status==200) assertEquals("response",request.goAndThrow());
            } finally { server.stop(0); }
        }
    }
    @Test void delayedHeadersAndStalledOrTricklingBodiesRespectTheTotalDeadline() throws Exception {
        var http=CoreServicesRaw.services().http();
        for (Mode mode:Mode.values()) {
            try(var peer=new Peer(mode)) {
                var response=assertTimeoutPreemptively(Duration.ofSeconds(5),() -> http.get(peer.url()).tryCount(4)
                        .totalTimeout(Duration.ofMillis(500)).goStream(upstream -> upstream.body().transferTo(OutputStream.nullOutputStream())));
                assertTrue(response.isRight()); assertInstanceOf(HttpTimeoutException.class,response.right());
                assertEquals(1,peer.calls.get());
                peer.release.countDown(); assertTrue(peer.disconnected.await(3,TimeUnit.SECONDS));
            }
        }
    }
    @Test void interruptionDuringConsumptionCancelsTheResponseAndKeepsTheFlag() throws Exception {
        var http=CoreServicesRaw.services().http();
        try(var peer=new Peer(Mode.STALLED)) {
            var reading=new CountDownLatch(1); var interrupted=new AtomicBoolean(); var failure=new AtomicReference<Exception>();
            Thread caller=Thread.ofPlatform().start(() -> {
                var result=http.get(peer.url()).totalTimeout(Duration.ofSeconds(30)).goStream(upstream -> {
                    reading.countDown(); upstream.body().transferTo(OutputStream.nullOutputStream());
                });
                interrupted.set(Thread.currentThread().isInterrupted()); if(result.isRight()) failure.set(result.right());
            });
            try {
                assertTrue(reading.await(3,TimeUnit.SECONDS)); caller.interrupt(); caller.join(3000);
                assertFalse(caller.isAlive()); assertTrue(interrupted.get()); assertInstanceOf(IOException.class,failure.get());
                peer.release.countDown(); assertTrue(peer.disconnected.await(3,TimeUnit.SECONDS));
            } finally { caller.interrupt(); caller.join(3000); }
        }
    }
    @Test void stalledBodiesTimeOutWhileEverySharedSchedulerThreadIsOccupied() throws Exception {
        var core = new CoreServicesRaw();
        var shared = new ScheduledThreadPoolExecutor(2);
        var scheduler = new IScheduledExecutorServiceImpl(shared);
        var async = new AsyncImpl() {
            @Override public IScheduledExecutorService scheduledExec() { return scheduler; }
        };
        var http = new HttpImpl(core.repeat(), core.times(), async, core.ids(), core.bytes());
        var occupied = new CountDownLatch(shared.getCorePoolSize());
        var release = new CountDownLatch(1);
        try {
            for (int i = 0; i < shared.getCorePoolSize(); i++) shared.execute(() -> {
                occupied.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            assertTrue(occupied.await(3, TimeUnit.SECONDS));
            for (boolean streaming : new boolean[]{true, false}) {
                try (var peer = new Peer(Mode.STALLED)) {
                    var request = http.get(peer.url()).tryCount(1).totalTimeout(Duration.ofMillis(500));
                    var response = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> streaming
                            ? request.goStream(upstream -> upstream.body().transferTo(OutputStream.nullOutputStream()))
                            : request.maxResponseBytes(1024).goResponse());
                    assertTrue(response.isRight()); assertInstanceOf(HttpTimeoutException.class, response.right());
                    peer.release.countDown(); assertTrue(peer.disconnected.await(3, TimeUnit.SECONDS));
                }
            }
        } finally { release.countDown(); shared.shutdownNow(); async.stop(); core.async().stop(); }
    }
    @Test void httpInstancesSharingAsyncKeepTheirActiveDeadlines() throws Exception {
        var core = new CoreServicesRaw();
        var callers = Executors.newFixedThreadPool(2);
        try (var firstPeer = new Peer(Mode.STALLED); var secondPeer = new Peer(Mode.STALLED)) {
            var reading = new CountDownLatch(1);
            var first = callers.submit(() -> core.http().get(firstPeer.url()).totalTimeout(Duration.ofSeconds(2))
                    .goStream(upstream -> { reading.countDown(); upstream.body().transferTo(OutputStream.nullOutputStream()); }));
            assertTrue(reading.await(3, TimeUnit.SECONDS));
            var other = new HttpImpl(core);
            var second = callers.submit(() -> other.get(secondPeer.url()).totalTimeout(Duration.ofMillis(500))
                    .goStream(upstream -> upstream.body().transferTo(OutputStream.nullOutputStream())));
            for (var pending : java.util.List.of(second, first)) {
                var response = pending.get(4, TimeUnit.SECONDS);
                assertTrue(response.isRight()); assertInstanceOf(HttpTimeoutException.class, response.right());
            }
        } finally { callers.shutdownNow(); core.async().stop(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS)); }
    }
    @Test void earlyReturnAndCallbackFailureCloseTheScopedBody() throws Exception {
        var http=CoreServicesRaw.services().http();
        for(boolean fail:new boolean[]{false,true}) {
            try(var peer=new Peer(Mode.STALLED)) {
                var failure=new IOException("callback failed"); var scoped=new AtomicReference<InputStream>();
                var response=http.get(peer.url()).tryCount(5).totalTimeout(Duration.ofSeconds(5)).goStream(upstream -> {
                    scoped.set(upstream.body()); assertEquals('x',upstream.body().read());
                    if(fail) throw failure;
                });
                if(fail) assertSame(failure,response.right()); else assertTrue(response.isLeft());
                assertThrows(IOException.class,() -> scoped.get().read());
                peer.release.countDown(); assertTrue(peer.disconnected.await(3,TimeUnit.SECONDS)); assertEquals(1,peer.calls.get());
            }
        }
    }
    @Test void connectionTimeoutDoesNotCutShortTheHeaderOrBodyDeadline() throws Exception {
        var http = CoreServicesRaw.services().http();
        var peer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        peer.createContext("/", exchange -> {
            try {
                Thread.sleep(200); exchange.sendResponseHeaders(200, 2);
                try (var output = exchange.getResponseBody()) { output.write('O'); output.flush(); Thread.sleep(200); output.write('K'); }
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        }); peer.start();
        try {
            for (boolean redirects : new boolean[]{true, false}) {
                var body = new AtomicReference<String>();
                var result = http.get(url(peer)).followRedirects(redirects).connectTimeout(Duration.ofMillis(100))
                        .totalTimeout(Duration.ofSeconds(3)).goStream(response -> body.set(new String(response.body().readAllBytes())));
                assertTrue(result.isLeft()); assertEquals("OK", body.get());
            }
        } finally { peer.stop(0); }
    }
    @Test void boundedBufferedResponsesLimitDecodedContent() throws Exception {
        var http=CoreServicesRaw.services().http();
        byte[] content=new byte[8192]; java.util.Arrays.fill(content,(byte)'x');
        var compressed=new ByteArrayOutputStream(); try(var gzip=new GZIPOutputStream(compressed)) { gzip.write(content); }
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding","gzip");
            exchange.sendResponseHeaders(200,compressed.size());
            try(var body=exchange.getResponseBody()) { body.write(compressed.toByteArray()); }
        }); server.start();
        try {
            var limited=http.get(url(server)).maxResponseBytes(1024).goResponse();
            assertTrue(limited.isRight()); assertInstanceOf(HttpResponseLimitException.class,limited.right());
            assertArrayEquals(content,http.get(url(server)).maxResponseBytes(content.length).goBytesAndThrow());
        } finally { server.stop(0); }
    }
    private String url(HttpServer server) { return "http://127.0.0.1:"+server.getAddress().getPort()+"/"; }
    private enum Mode { HEADERS, STALLED, TRICKLE }
    private final class Peer implements AutoCloseable {
        final CountDownLatch release=new CountDownLatch(1), disconnected=new CountDownLatch(1);
        final AtomicInteger calls=new AtomicInteger();
        final ExecutorService workers=Executors.newCachedThreadPool();
        final HttpServer server;
        Peer(Mode mode) throws IOException {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); server.setExecutor(workers);
            server.createContext("/",exchange -> {
                calls.incrementAndGet();
                try {
                    if(mode==Mode.HEADERS) release.await();
                    exchange.sendResponseHeaders(200,0); var output=exchange.getResponseBody(); output.write('x'); output.flush();
                    while(!release.await(20,TimeUnit.MILLISECONDS)) if(mode==Mode.TRICKLE) { output.write('x'); output.flush(); }
                    long end=System.nanoTime()+Duration.ofSeconds(3).toNanos();
                    while(System.nanoTime()<end) { output.write(new byte[8192]); output.flush(); Thread.sleep(10); }
                } catch(IOException closed) { disconnected.countDown(); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            }); server.start();
        }
        String url() { return HttpStreamingTest.this.url(server); }
        @Override public void close() { release.countDown(); server.stop(0); workers.shutdownNow(); }
    }
}
