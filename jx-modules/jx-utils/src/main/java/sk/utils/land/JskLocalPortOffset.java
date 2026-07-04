package sk.utils.land;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public final class JskLocalPortOffset {
    public static final String ENV_VAR = "RGS_PORT_OFFSET";
    public static final String SYS_PROP = "rgs.port.offset";
    public static final String FILE_NAME = ".rgs-port-offset";

    private static volatile Integer cached;

    private JskLocalPortOffset() {
    }

    public static int getOffset() {
        Integer local = cached;
        if (local == null) {
            synchronized (JskLocalPortOffset.class) {
                if (cached == null) {
                    cached = resolveAndLog();
                }
                local = cached;
            }
        }
        return local;
    }

    public static String pathSuffix() {
        int offset = getOffset();
        return offset == 0 ? "" : "-" + offset;
    }

    private static int resolveAndLog() {
        String fromSysProp = System.getProperty(SYS_PROP);
        if (isNotBlank(fromSysProp)) {
            Integer parsed = parse(fromSysProp);
            if (parsed != null) {
                return logged(parsed, "-D" + SYS_PROP);
            }
        }

        String fromEnv = System.getenv(ENV_VAR);
        if (isNotBlank(fromEnv)) {
            Integer parsed = parse(fromEnv);
            if (parsed != null) {
                return logged(parsed, "env " + ENV_VAR);
            }
        }

        String workDir = System.getProperty("user.dir");
        Path start = isNotBlank(workDir) ? Path.of(workDir).toAbsolutePath() : null;

        for (Path dir = start; dir != null; dir = dir.getParent()) {
            Path file = dir.resolve(FILE_NAME);
            if (Files.isRegularFile(file)) {
                Integer parsed = readOffsetFile(file);
                if (parsed != null) {
                    return logged(parsed, file.toString());
                }
            }
        }

        return 0;
    }

    private static Integer readOffsetFile(Path file) {
        try {
            return parse(Files.readString(file));
        } catch (Exception e) {
            log.warn("Could not read port offset file {}: {}", file, e.toString());
            return null;
        }
    }

    private static int logged(int offset, String source) {
        log.info("Local port offset = {} (source: {})", offset, source);
        return offset;
    }

    private static Integer parse(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
