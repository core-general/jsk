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

import java.io.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Subscription;
import java.util.zip.GZIPInputStream;

/** File bodies complete only at EOF, so the total deadline includes reception of the body. */
final class HttpFileTransfer {
    private static final int ERROR_BYTES = 64 * 1024;

    record Result(int code, Map<String, List<String>> headers, byte[] errorBody) {}

    static Result download(HttpClient client, HttpRequest request, IHttp.HttpBuilder<?> options,
                           boolean decodeGzip) throws IOException, InterruptedException {
        var destination = options.fileDestination();
        Path target = destination.path();
        // A private staging directory prevents retry/cancellation from touching caller files.
        if (Files.exists(target)) throw new FileAlreadyExistsException(target.toString());
        Path staging = Files.createTempDirectory(target.toAbsolutePath().getParent(), ".http-download-");
        Path wire = staging.resolve("wire");
        Path decoded = staging.resolve("decoded");
        long started = System.nanoTime();
        long budget = options.totalTimeout().isPresent()
                ? options.totalTimeout().get().toNanos() : Long.MAX_VALUE;
        var sink = new FileSubscriber(wire, destination);
        CompletableFuture<HttpResponse<byte[]>> future = null;
        try {
            future = client.sendAsync(request, info -> {
                sink.configure(info.statusCode() >= 200 && info.statusCode() < 300);
                return sink;
            });
            HttpResponse<byte[]> response;
            try {
                response = budget == Long.MAX_VALUE ? future.get()
                        : future.get(remaining(started, budget), TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                throw new HttpTimeoutException("Total HTTP request timeout exceeded");
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException failure) throw failure;
                if (e.getCause() instanceof RuntimeException failure) throw failure;
                if (e.getCause() instanceof Error failure) throw failure;
                throw new IOException(e.getCause());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                byte[] preview = response.body();
                if (decodeGzip && gzip(response)) {
                    try (var input = new GZIPInputStream(new ByteArrayInputStream(preview))) {
                        preview = input.readNBytes(ERROR_BYTES);
                    } catch (IOException ignored) { /* A truncated preview may not be decodable. */ }
                }
                return new Result(response.statusCode(), response.headers().map(), preview);
            }
            Path body = wire;
            if (decodeGzip && gzip(response)) {
                try (var input = new GZIPInputStream(Files.newInputStream(wire));
                     var output = Files.newOutputStream(decoded, StandardOpenOption.CREATE_NEW)) {
                    byte[] buffer = new byte[8192];
                    long total = 0;
                    for (int n; (n = input.read(buffer)) != -1;) {
                        checkDeadline(started, budget);
                        if (n > destination.maxBytes() - total)
                            throw new HttpResponseLimitException(destination.maxBytes());
                        destination.reserveBytes().accept(n);
                        output.write(buffer, 0, n);
                        total += n;
                    }
                }
                body = decoded;
            }
            checkDeadline(started, budget);
            Files.move(body, target);
            return new Result(response.statusCode(), response.headers().map(), new byte[0]);
        } finally {
            // Close the subscriber before deleting staging files, including when interrupted.
            sink.abort();
            if (future != null && !future.isDone()) future.cancel(true);
            Files.deleteIfExists(wire);
            Files.deleteIfExists(decoded);
            Files.deleteIfExists(staging);
        }
    }

    private static boolean gzip(HttpResponse<?> response) {
        return response.headers().allValues("Content-Encoding").stream()
                .anyMatch(value -> value.equalsIgnoreCase("gzip"));
    }

    private static long remaining(long started, long budget) {
        return Math.max(0, budget - (System.nanoTime() - started));
    }

    private static void checkDeadline(long started, long budget) throws IOException, InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        if (budget != Long.MAX_VALUE && remaining(started, budget) == 0)
            throw new HttpTimeoutException("Total HTTP request timeout exceeded");
    }

    private static final class FileSubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final Path path;
        private final IHttp.HttpFileDestination destination;
        private final ByteArrayOutputStream preview = new ByteArrayOutputStream();
        private Subscription subscription;
        private OutputStream output;
        private boolean successful;
        private boolean finished;
        private long count;

        FileSubscriber(Path path, IHttp.HttpFileDestination destination) {
            this.path = path;
            this.destination = destination;
        }

        synchronized void configure(boolean successful) { this.successful = successful; }

        @Override public CompletionStage<byte[]> getBody() { return result; }

        @Override public synchronized void onSubscribe(Subscription subscription) {
            if (finished || this.subscription != null) { subscription.cancel(); return; }
            this.subscription = subscription;
            try {
                if (successful) output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW);
                subscription.request(1);
            } catch (Exception e) { fail(e); }
        }

        @Override public synchronized void onNext(List<ByteBuffer> buffers) {
            if (finished) return;
            try {
                byte[] scratch = new byte[8192];
                for (ByteBuffer buffer : buffers) {
                    while (buffer.hasRemaining()) {
                        int n = Math.min(buffer.remaining(), scratch.length);
                        if (!successful) n = Math.min(n, ERROR_BYTES - preview.size());
                        buffer.get(scratch, 0, n);
                        if (successful) {
                            if (n > destination.maxBytes() - count)
                                throw new HttpResponseLimitException(destination.maxBytes());
                            destination.reserveBytes().accept(n);
                            output.write(scratch, 0, n);
                            count += n;
                        } else {
                            preview.write(scratch, 0, n);
                            if (preview.size() == ERROR_BYTES) {
                                subscription.cancel();
                                complete();
                                return;
                            }
                        }
                    }
                }
                subscription.request(1);
            } catch (Exception e) { fail(e); }
        }

        @Override public synchronized void onError(Throwable error) { fail(error); }
        @Override public synchronized void onComplete() { complete(); }

        private void complete() {
            if (finished) return;
            try {
                closeOutput();
                finished = true;
                result.complete(preview.toByteArray());
            } catch (IOException e) { fail(e); }
        }

        private void fail(Throwable error) {
            if (finished) return;
            finished = true;
            if (subscription != null) subscription.cancel();
            try { closeOutput(); } catch (IOException close) { error.addSuppressed(close); }
            result.completeExceptionally(error);
        }

        private void closeOutput() throws IOException {
            if (output != null) {
                OutputStream closing = output;
                output = null;
                closing.close();
            }
        }

        synchronized void abort() { fail(new IOException("HTTP file transfer closed")); }
    }
}
