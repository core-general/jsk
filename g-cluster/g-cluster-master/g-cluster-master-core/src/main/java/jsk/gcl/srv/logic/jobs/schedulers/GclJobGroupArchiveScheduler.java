package jsk.gcl.srv.logic.jobs.schedulers;

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

import jsk.gcl.srv.jpa.GclJobGroup;
import jsk.gcl.srv.logic.jobs.storage.GclJobArchiveStorage;
import jsk.gcl.srv.logic.jobs.storage.GclJobStorage;
import lombok.extern.log4j.Log4j2;
import sk.services.boot.IBoot;
import sk.services.clusterworkers.kvonofflocker.CluKvBasedOnOffWithLockWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.O;
import sk.utils.tuples.X;

import javax.inject.Inject;
import java.util.List;

import static sk.utils.statics.Ti.minute;
import static sk.utils.statics.Ti.second;

/**
 * 1. Checks job groups which are finished
 * 2. Archive them
 */
@Log4j2
public class GclJobGroupArchiveScheduler extends CluKvBasedOnOffWithLockWorker<CluKvBasedOnOffWithLockWorker.IConf>
        implements IBoot {

    @Inject GclJobStorage jobStorage;
    @Inject GclJobArchiveStorage archiveStorage;

    public GclJobGroupArchiveScheduler() {
        super("GclJobGroupArchiveScheduler");
    }

    @Override
    public void run() {
        start(new CluKvBasedOnOffWithLockWorker.ConfAlwaysOn(
                30 * second, 2 * minute, false,
                CluDelay.fixed(1 * minute),
                this::mainTask,
                e -> log.error("", e)
        ));
    }

    private void mainTask(CancelGetter cancelGetter) {
        final List<GclJobGroup> finishedOrFailed = jobStorage.getFinishedOrFailedJobGroups();
        for (GclJobGroup gclJobGroup : finishedOrFailed) {
            try {
                jobStorage.transactionWithSaveX1(() -> {
                    jobStorage.getJobGroupById(gclJobGroup.getJgId()).ifPresent(jg -> {
                        archiveStorage.moveJobGroupToArchive(jg);
                    });
                    return X.x(O.empty());
                });
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }
}
