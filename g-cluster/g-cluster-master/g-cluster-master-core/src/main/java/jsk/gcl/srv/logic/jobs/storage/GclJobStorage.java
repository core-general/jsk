package jsk.gcl.srv.logic.jobs.storage;

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
import jsk.gcl.cli.model.GclJobGroupId;
import jsk.gcl.cli.model.GclJobId;
import jsk.gcl.srv.jpa.GclJob;
import jsk.gcl.srv.jpa.GclJobGroup;
import sk.db.relational.spring.services.RdbTransactionManager;
import sk.utils.functional.O;

import java.util.List;

public interface GclJobStorage extends RdbTransactionManager {
    O<GclJobGroup> getJobGroupById(GclJobGroupId jobGroupId);

    List<GclJobGroup> getFinishedOrFailedJobGroups();

    GclJobGroup newGclJobGroup(GclJobGroupDto dto);

    O<GclJob> getJobById(GclJobId jobId);

    List<GclJob> getJobsRelatedTo(GclJobGroupId jobGroupId);

    GclJob newGclJob(GclJobGroupId jJgId, GclJobDto<?, ?, ?> job);

    void incrementJobGroupSuccess(GclJobGroupId jJgId);

    void incrementJobGroupFail(GclJobGroupId jJgId);

    void updateLifePings(List<GclJobId> activeJobs);

    long getActiveJobCount();

    void updateTasksIfLifePingIsOld();
}
