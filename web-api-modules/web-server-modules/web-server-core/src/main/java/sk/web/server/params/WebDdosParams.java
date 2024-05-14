package sk.web.server.params;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import sk.utils.functional.O;

import java.time.Duration;

public interface WebDdosParams {
    /**
     * Amount of time, user is in DDOS court to decide, whether he is ok or not.
     * Once this period finishes, user is dropped from the court and enters the court with fresh state.
     * Amount of requests in court is defined by getUserRequestsAllowedInCourt
     */
    Duration getUserInCourtPeriod();

    /**
     * Number of requests allowed during in court. If more requests issued, the user is banned
     */
    public int getUserRequestsAllowedInCourt();

    Duration getUserInJailTime();

    boolean isDdosCourtEnabled();

    default O<String> getDdosPassingHeader() {
        return O.empty();
    }
}
