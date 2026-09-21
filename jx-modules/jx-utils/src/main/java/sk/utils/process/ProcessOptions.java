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

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record ProcessOptions(List<String> command, Path directory, Map<String, String> environment,
                             Duration timeout, Duration terminationGrace, int outputLimit, boolean newSession) {
    public ProcessOptions {
        command = List.copyOf(command);
        environment = Map.copyOf(environment);
        if (command.isEmpty() || timeout.isNegative() || timeout.isZero()
                || terminationGrace.isNegative() || outputLimit < 0) {
            throw new IllegalArgumentException("Invalid process options");
        }
    }
}
