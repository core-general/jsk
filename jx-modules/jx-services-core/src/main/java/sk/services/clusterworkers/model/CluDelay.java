package sk.services.clusterworkers.model;

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
import sk.services.time.CronExpression;
import sk.services.time.ITime;
import sk.utils.functional.F0;
import sk.utils.statics.Ex;

import java.util.Date;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CluDelay {
    F0<Long> delay;
    boolean isStatic;

    public static CluDelay cron(ITime times, String cron) {
        CronExpression ce = Ex.toRuntime(() -> new CronExpression(cron));
        return new CluDelay(() -> {
            long now = times.now();
            long nextValid = ce.getNextValidTimeAfter(new Date(now)).getTime();
            return nextValid - now;
        }, false);
    }

    public static CluDelay fixed(long msDelay) {
        return new CluDelay(() -> msDelay, true);
    }

    @SuppressWarnings("unused")
    public static CluDelay dynamic(F0<Long> delayProvider) {
        return new CluDelay(delayProvider, false);
    }
}
