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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

public interface WebServerParams {
    default String getServerNameHeader() {
        return "";
    }

    int getPort();

    O<Long> getShutdownWait();

    long getFormLimit();

    O<Long> getIdleTimeout();

    O<WebStaticFiles> getStaticFilesLocation();

    O<Integer> getTokenTimeoutSec();

    boolean isUseCookiesForToken();

    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class WebStaticFiles {
        @Getter private OneOf<String, String> eitherResourceOrExternal;

        public static WebStaticFiles resources(String resourceDir) {
            return new WebStaticFiles(OneOf.left(resourceDir));
        }

        public static WebStaticFiles externalFolder(String externalDir) {
            return new WebStaticFiles(OneOf.right(externalDir));
        }
    }
}
