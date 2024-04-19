package sk.services.shutdown;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import jakarta.inject.Inject;
import lombok.Getter;
import sk.services.async.IAsync;
import sk.services.boot.IBoot;
import sk.services.log.ILog;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;

import java.util.List;
import java.util.Optional;

import static sk.services.log.ILogCatDefault.DEFAULT;

public class AppStopService implements IBoot {
    @Inject Optional<List<AppStopListener>> toExit = Optional.empty();
    @Inject IAsync async;
    @Inject ILog log;

    @Getter private volatile boolean stopped = false;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopped = true;
            O.of(toExit).stream().flatMap($ -> $.stream()).forEach($ -> {
                try {
                    $.onStop();
                } catch (Throwable e) {
                    log.logError(DEFAULT, "SHUTDOWN_ERROR", Cc.m("e", Ex.getInfo(e)));
                }
            });
            try {
                O.of(toExit).stream().flatMap($ -> $.stream())
                        .mapToLong(AppStopListener::waitBeforeStopMs)
                        .max()
                        .ifPresent($ -> async.sleep($));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }));
    }
}
