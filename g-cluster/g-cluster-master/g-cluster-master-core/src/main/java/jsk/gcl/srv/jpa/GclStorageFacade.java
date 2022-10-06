
package jsk.gcl.srv.jpa;

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
import jsk.gcl.cli.model.GclJobGroupDto;
import jsk.gcl.srv.scaling.storage.GclJobArchiveStorage;
import jsk.gcl.srv.scaling.storage.GclJobStorage;
import jsk.gcl.srv.scaling.storage.GclNodeArchiveStorage;
import jsk.gcl.srv.scaling.storage.GclNodeStorage;
import sk.db.relational.spring.services.RdbTransactionManager;
import sk.utils.functional.O;

import javax.transaction.Transactional;
import java.util.List;

public interface GclStorageFacade extends RdbTransactionManager,
                                          GclNodeStorage, GclJobArchiveStorage,
                                          GclJobStorage,
                                          GclNodeArchiveStorage {
    O<GclJobGroup> getJobGroupById(GclJobGroupId jobGroupId);

    GclJobGroup newGclJobGroup(GclJobGroupDto dto);

    O<GclJob> getJobById(GclJobId jobId);

    List<GclJob> getJobsRelatedTo(GclJobGroupId jobGroupId);

    GclJob newGclJob(GclJobGroupId jJgId, GclJobDto<?, ?> job);

    O<GclJobGroupArchive> getJobGroupArchiveById(GclJobGroupArchiveId jobGroupArchiveId);

    void moveJobGroupToArchive(GclJobGroup group);

    O<GclNode> getNodeById(GclNodeId nodeId);

    O<GclNodeArchive> getNodeArchiveById(GclNodeArchiveId nodeArchiveId);

    @Transactional
    void archiveNode(GclNode archiveNode);
}
