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

import jsk.gcl.cli.model.GclJobId;
import jsk.gcl.srv.logic.jobs.services.GclJobManager;
import jsk.gcl.srv.logic.jobs.storage.GclJobStorage;
import jsk.gcl.srv.logic.scaling.storage.GclNodeStorage;
import lombok.extern.log4j.Log4j2;
import sk.services.boot.IBoot;
import sk.services.clusterworkers.kvonoff.CluKvBasedOnOffWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.utils.async.cancel.CancelGetter;

import javax.inject.Inject;
import java.util.List;

import static sk.utils.statics.Ti.second;

/**
 * Updates life pings for jobs, which are taken by this node
 */
@Log4j2
public class GclJobUpdateLifePingScheduler extends CluKvBasedOnOffWorker<CluKvBasedOnOffWorker.IConf> implements IBoot {

    public GclJobUpdateLifePingScheduler() {
        super("GclJobLockScheduler");
    }

    @Inject GclJobManager jobManager;
    @Inject GclJobStorage jobs;
    @Inject GclNodeStorage nodes;

    @Override
    public void run() {
        start(new Conf(
                30 * second, CluDelay.fixed(15 * second),
                this::updateLifePings,
                e -> log.error("", e)
        ));
    }

    private void updateLifePings(CancelGetter cancelGetter) {
        List<GclJobId> jobs = jobManager.getAllNodeJobs();

        jobs.updateLigePings(jobs);
    }
}
