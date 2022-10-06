package jsk.gcl.srv.scaling.schedulers;

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

import jsk.gcl.srv.scaling.GclNodeArchiveManager;
import sk.services.boot.IBoot;
import sk.services.clusterworkers.kvonofflocker.CluKvBasedOnOffWithLockWorker;


/**
 * 1. Checks all nodes in a active node table
 * 2. If node does not update it's data for a long time, moves node to archive
 */
public class GclNodeArchiveScheduler extends CluKvBasedOnOffWithLockWorker<CluKvBasedOnOffWithLockWorker.IConf>
        implements IBoot, GclNodeArchiveManager {
    public GclNodeArchiveScheduler() {
        super("GclNodeArchiveScheduler");
    }

    @Override
    public void run() {
        //todo
    }

    @Override
    public void archiveNode(String nodeId) {
        //todo
    }
}
