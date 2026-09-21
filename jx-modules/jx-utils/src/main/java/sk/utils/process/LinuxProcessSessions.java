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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LinuxProcessSessions {
    public record Identity(long pid, long group, long session, long startTicks) {}

    public static Identity identity(long pid) throws IOException {
        String stat = Files.readString(Path.of("/proc", Long.toString(pid), "stat"));
        String[] fields = stat.substring(stat.lastIndexOf(')') + 2).split(" ");
        return new Identity(pid, Long.parseLong(fields[2]), Long.parseLong(fields[3]), Long.parseLong(fields[19]));
    }

    public static List<Identity> findOwned(String key, String value) throws IOException {
        List<Identity> found = new ArrayList<>();
        try (var paths = Files.list(Path.of("/proc"))) {
            for (Path path : paths.filter(p -> p.getFileName().toString().matches("[0-9]+")).toList()) {
                try {
                    byte[] environment = Files.readAllBytes(path.resolve("environ"));
                    String expected = key + "=" + value;
                    for (String entry : new String(environment, StandardCharsets.UTF_8).split("\u0000")) {
                        if (entry.equals(expected)) {
                            found.add(identity(Long.parseLong(path.getFileName().toString())));
                            break;
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return found;
    }

    public static boolean hasLiveMembers(long group) throws IOException {
        return liveProcesses().stream().anyMatch(process -> process.group() == group);
    }

    public static List<Identity> liveSessionMembers(long session) throws IOException {
        return liveProcesses().stream().filter(process -> process.session() == session).toList();
    }

    private static List<Identity> liveProcesses() throws IOException {
        List<Identity> found = new ArrayList<>();
        try (var paths = Files.list(Path.of("/proc"))) {
            for (Path path : paths.filter(p -> p.getFileName().toString().matches("[0-9]+")).toList()) {
                try {
                    String stat = Files.readString(path.resolve("stat"));
                    String[] fields = stat.substring(stat.lastIndexOf(')') + 2).split(" ");
                    if (!fields[0].equals("Z") && !fields[0].equals("X")) {
                        found.add(new Identity(Long.parseLong(path.getFileName().toString()), Long.parseLong(fields[2]),
                                Long.parseLong(fields[3]), Long.parseLong(fields[19])));
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return found;
    }

    public static void terminateSession(long session, Duration grace) throws IOException, InterruptedException {
        if (session <= 1 || session == identity(ProcessHandle.current().pid()).session()) {
            throw new IOException("Refusing to signal the current or an invalid process session");
        }
        var members = liveSessionMembers(session);
        if (members.isEmpty()) return;
        for (long group : members.stream().map(Identity::group).distinct().toList()) signal(group, "TERM");
        long deadline = System.nanoTime() + grace.toNanos();
        while (!liveSessionMembers(session).isEmpty() && System.nanoTime() < deadline) Thread.sleep(20);
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!(members = liveSessionMembers(session)).isEmpty()) {
            if (System.nanoTime() >= deadline) throw new IOException("Process session did not terminate: " + session);
            for (long group : members.stream().map(Identity::group).distinct().toList()) signal(group, "KILL");
            Thread.sleep(20);
        }
    }

    public static void terminateGroup(long group, Duration grace) throws IOException, InterruptedException {
        if (group <= 1 || group == identity(ProcessHandle.current().pid()).group()) {
            throw new IOException("Refusing to signal the current or an invalid process group");
        }
        if (!hasLiveMembers(group)) return;
        signal(group, "TERM");
        awaitExit(group, grace);
        if (hasLiveMembers(group)) {
            signal(group, "KILL");
            awaitExit(group, Duration.ofSeconds(5));
        }
        if (hasLiveMembers(group)) throw new IOException("Process group did not terminate: " + group);
    }

    private static void signal(long group, String signal) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("/bin/kill", "-" + signal, "--", "-" + group)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Timed out signaling process group " + group);
        }
    }

    private static void awaitExit(long group, Duration duration) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + duration.toNanos();
        while (hasLiveMembers(group) && System.nanoTime() < deadline) Thread.sleep(20);
    }

    private LinuxProcessSessions() {}
}
