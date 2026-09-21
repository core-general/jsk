package sk.utils.statics;

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
import org.junit.jupiter.api.io.TempDir;
import sk.utils.process.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IoProcessTest {
    @TempDir Path temporary;

    @Test void sendsLiteralInputInTheConfiguredDirectoryAndEnvironment() throws Exception {
        Path work = Files.createDirectory(temporary.resolve("space in path"));
        Path script = Files.writeString(temporary.resolve("worker.sh"), "cat > input.txt\nprintf '%s' \"$EXE_WORK_DIR\"\n");
        String prompt = "quotes ' \" and $(touch unwanted) | pipe\nUnicode: λ\n";
        var options = new ProcessOptions(List.of("/bin/bash", script.toString()), work,
                Map.of("EXE_WORK_DIR", work.toString()), Duration.ofSeconds(5), Duration.ofMillis(50), 2048, true);
        try (var process = Io.startProcess(options)) {
            var result = process.await(prompt);
            assertEquals(0, result.exitCode());
            assertEquals(prompt, Files.readString(work.resolve("input.txt")));
            assertEquals(work.toString(), result.output());
            assertFalse(Files.exists(work.resolve("unwanted")));
        }
    }

    @Test void timesOutBlockedInputAndTerminatesTheWholeSession() throws Exception {
        Path script = Files.writeString(temporary.resolve("worker.sh"), "sleep 60 &\necho $! > child.pid\nwait\n");
        var options = new ProcessOptions(List.of("/bin/bash", script.toString()), temporary, Map.of(),
                Duration.ofMillis(300), Duration.ofMillis(30), 64, true);
        try (var process = Io.startProcess(options)) {
            var result = process.await("input".repeat(100000));
            assertTrue(result.timedOut());
            assertFalse(LinuxProcessSessions.hasLiveMembers(process.pid()));
        }
    }

    @Test void drainsExcessOutputAndKeepsItsTail() throws Exception {
        Path script = Files.writeString(temporary.resolve("worker.sh"), "cat >/dev/null\nhead -c 200000 /dev/zero\nprintf 'final-message'\n");
        int limit = 64;
        var options = new ProcessOptions(List.of("/bin/bash", script.toString()), temporary, Map.of(),
                Duration.ofSeconds(5), Duration.ZERO, limit, true);
        try (var process = Io.startProcess(options)) {
            var result = process.await("");
            assertEquals(0, result.exitCode());
            assertTrue(result.outputTruncated());
            assertEquals(limit, result.output().length());
            assertTrue(result.output().endsWith("final-message"));
        }
    }

    @Test void ownershipDiscoveryIncludesAnUnregisteredLaunch() throws Exception {
        String owner = java.util.UUID.randomUUID().toString();
        var options = new ProcessOptions(List.of("/bin/bash", "-c", "sleep 60"), temporary,
                Map.of("TEST_PROCESS_OWNER", owner), Duration.ofSeconds(5), Duration.ZERO, 64, true);
        try (var process = Io.startProcess(options)) {
            long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
            List<LinuxProcessSessions.Identity> found;
            do {
                found = LinuxProcessSessions.findOwned("TEST_PROCESS_OWNER", owner);
                if (!found.isEmpty()) break;
                Thread.sleep(10);
            } while (System.nanoTime() < deadline);
            assertTrue(found.stream().anyMatch(p -> p.pid() == process.pid()));
        }
    }

    @Test void timeoutTerminatesChildrenInOtherGroupsOfTheOwnedSession() throws Exception {
        Path childPid = temporary.resolve("child.pid");
        Path script = Files.writeString(temporary.resolve("job-control.sh"),
                "set -m\ncat >/dev/null\nsleep 60 &\necho $! > child.pid\nwait\n");
        var options = new ProcessOptions(List.of("/bin/bash", script.toString()), temporary, Map.of(),
                Duration.ofMillis(500), Duration.ofMillis(30), 64, true);
        try (var process = Io.startProcess(options)) {
            try {
                var result = process.await("prompt");
                assertTrue(result.timedOut());
                long child = Long.parseLong(Files.readString(childPid).strip());
                assertNotEquals(process.pid(), child);
                assertTrue(LinuxProcessSessions.liveSessionMembers(process.pid()).isEmpty());
                assertFalse(LinuxProcessSessions.hasLiveMembers(child));
            } finally {
                if (Files.exists(childPid)) ProcessHandle.of(Long.parseLong(Files.readString(childPid).strip()))
                        .ifPresent(ProcessHandle::destroyForcibly);
            }
        }
    }
}
