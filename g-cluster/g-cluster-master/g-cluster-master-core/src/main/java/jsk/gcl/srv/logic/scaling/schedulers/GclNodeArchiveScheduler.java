package jsk.gcl.srv.logic.scaling.schedulers;

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

import jsk.gcl.srv.jpa.GclNode;
import jsk.gcl.srv.jpa.GclNodeId;
import jsk.gcl.srv.logic.scaling.GclNodeArchiveManager;
import jsk.gcl.srv.logic.scaling.storage.GclNodeArchiveStorage;
import jsk.gcl.srv.logic.scaling.storage.GclNodeStorage;
import lombok.extern.log4j.Log4j2;
import sk.services.boot.IBoot;
import sk.services.clusterworkers.kvonofflocker.CluKvBasedOnOffWithLockWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.utils.async.cancel.CancelGetter;

import javax.inject.Inject;
import java.util.List;

import static sk.utils.statics.Ti.minute;
import static sk.utils.statics.Ti.second;


/**
 * 1. Checks all nodes in a active node table
 * 2. If node does not update it's data for a long time, moves node to archive
 */
@Log4j2
public class GclNodeArchiveScheduler extends CluKvBasedOnOffWithLockWorker<CluKvBasedOnOffWithLockWorker.IConf>
        implements IBoot, GclNodeArchiveManager {

    @Inject GclNodeStorage nodeStorage;
    @Inject GclNodeArchiveStorage archive;

    public GclNodeArchiveScheduler() {
        super("GclNodeArchiveScheduler");
    }

    @Override
    public void run() {
        start(new CluKvBasedOnOffWithLockWorker.ConfAlwaysOn(
                30 * second, 2 * minute, false,
                CluDelay.fixed(10 * second),
                this::mainTask,
                e -> log.error("", e)
        ));
    }

    private void mainTask(CancelGetter cancel) {
        List<GclNode> allBadNodes = nodeStorage.getInactiveNodes();
        allBadNodes.stream().parallel().forEach($ -> {
            try {
                archive.archiveNode($.getNId());
            } catch (Exception e) {
                log.error("", e);
            }
        });
    }

    @Override
    public void archiveNode(GclNodeId nodeId) {
        archive.archiveNode(nodeId);
    }
}
