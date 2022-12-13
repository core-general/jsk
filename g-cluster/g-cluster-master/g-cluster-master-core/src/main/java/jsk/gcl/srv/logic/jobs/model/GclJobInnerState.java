package jsk.gcl.srv.logic.jobs.model;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import jsk.gcl.cli.model.GclJobDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import sk.utils.collections.DequeWithLimit;
import sk.utils.statics.Ex;

import javax.annotation.Nullable;

@NoArgsConstructor
@EqualsAndHashCode
public class GclJobInnerState {
    public static final String type = "jsk.gcl.srv.logic.jobs.model.GclJobInnerState";

    private GclJobDto<?, ?, ?> jobInfo;

    @Nullable private Object success;

    private int failCount;
    private DequeWithLimit<GclJobInnerStateFail> fails;

    private boolean finished;

    public GclJobInnerState(GclJobDto<?, ?, ?> jobInfo) {
        this.jobInfo = jobInfo;
        this.fails = new DequeWithLimit<>(5);
    }

    public boolean failAndContinue(Throwable err, int maxFailCount) {
        failCount++;
        fails.add(new GclJobInnerStateFail(Ex.getInfo(err)));
        finished = finished || failCount >= maxFailCount;
        return !finished;
    }

    public void succeed(Object success) {
        this.success = success;
        finished = true;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class GclJobInnerStateFail {
        String stackTrace;
    }
}
