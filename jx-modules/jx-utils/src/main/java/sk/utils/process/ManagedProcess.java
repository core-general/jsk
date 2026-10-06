package sk.utils.process;

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class ManagedProcess implements AutoCloseable {
    private final Process process;
    private final ProcessOptions options;
    private final long started = System.nanoTime();
    private final Tail output;
    private final Thread reader;
    private boolean closed;

    public ManagedProcess(ProcessOptions options) throws IOException {
        this.options = options;
        var command = new ArrayList<String>();
        if (options.newSession()) command.addAll(java.util.List.of("/usr/bin/setsid", "--wait", "--"));
        command.addAll(options.command());
        var builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.directory(options.directory().toFile());
        builder.environment().putAll(options.environment());
        process = builder.start();
        output = new Tail(options.outputLimit());
        reader = Thread.ofVirtual().name("process-output-" + process.pid()).start(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                for (int n; (n = in.read(buffer)) >= 0;) output.append(buffer, n);
            } catch (IOException ignored) {
            }
        });
    }

    public long pid() { return process.pid(); }
    public Instant startInstant() { return process.info().startInstant().orElse(Instant.EPOCH); }

    public ProcessResult await(String input) throws IOException, InterruptedException {
        AtomicReference<IOException> writeError = new AtomicReference<>();
        Thread writer = Thread.ofVirtual().name("process-input-" + pid()).start(() -> {
            try (var out = process.getOutputStream()) {
                out.write(input.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                writeError.set(e);
            }
        });
        boolean timedOut;
        try {
            long remaining = Math.max(0, options.timeout().toNanos() - (System.nanoTime() - started));
            timedOut = !process.waitFor(remaining, TimeUnit.NANOSECONDS);
        } finally {
            close();
            writer.join(5000);
            reader.join(5000);
        }
        if (!timedOut && process.exitValue() == 0 && writeError.get() != null) throw writeError.get();
        return new ProcessResult(process.exitValue(), timedOut, output.text(), output.truncated());
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        boolean[] interrupted = {Thread.interrupted()};
        try {
            var children = process.descendants().toList();
            if (options.newSession()) finishSession(interrupted);
            children.forEach(ProcessHandle::destroy);
            process.destroy();
            if (!awaitTermination(options.terminationGrace().toMillis(), interrupted)) process.destroyForcibly();
            children.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            if (!awaitTermination(5000, interrupted)) throw new IOException("Process did not terminate: " + pid());
            if (options.newSession()) finishSession(interrupted);
            closed = true;
        } catch (IOException e) {
            throw new ProcessTerminationException("Cannot verify process termination: " + pid(), e);
        } finally {
            if (interrupted[0]) Thread.currentThread().interrupt();
        }
    }

    private void finishSession(boolean[] interrupted) throws IOException {
        long deadline = System.nanoTime() + options.terminationGrace().toNanos() + TimeUnit.SECONDS.toNanos(5);
        java.time.Duration grace = options.terminationGrace();
        while (true) {
            try { LinuxProcessSessions.terminateSession(pid(), grace); return; }
            catch (InterruptedException cancellation) {
                interrupted[0] = true;
                if (System.nanoTime() >= deadline) throw new ProcessTerminationException("Process cleanup interrupted beyond its deadline", cancellation);
                grace = java.time.Duration.ZERO;
            }
        }
    }

    private boolean awaitTermination(long millis, boolean[] interrupted) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (true) {
            try { return process.waitFor(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS); }
            catch (InterruptedException cancellation) { interrupted[0] = true; }
        }
    }

    private static final class Tail {
        private final byte[] bytes;
        private long count;
        Tail(int size) { bytes = new byte[size]; }
        synchronized void append(byte[] source, int length) {
            for (int i = 0; i < length; i++) {
                if (bytes.length > 0) bytes[(int) (count % bytes.length)] = source[i];
                count++;
            }
        }
        synchronized String text() {
            int length = (int) Math.min(count, bytes.length);
            byte[] result = new byte[length];
            for (int i = 0; i < length; i++) result[i] = bytes[(int) ((count - length + i) % bytes.length)];
            return new String(result, StandardCharsets.UTF_8);
        }
        synchronized boolean truncated() { return count > bytes.length; }
    }
}
