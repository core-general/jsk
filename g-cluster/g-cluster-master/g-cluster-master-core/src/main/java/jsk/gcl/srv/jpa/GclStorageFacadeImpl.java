
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

import jakarta.transaction.Transactional;
import jsk.gcl.cli.model.GclJobDto;
import jsk.gcl.cli.model.GclJobGroupDto;
import jsk.gcl.cli.model.GclJobGroupId;
import jsk.gcl.cli.model.GclJobId;
import jsk.gcl.srv.logic.jobs.model.GclJobGroupInnerState;
import jsk.gcl.srv.logic.jobs.model.GclJobInnerState;
import jsk.gcl.srv.logic.jobs.model.GclJobStatus;
import jsk.gcl.srv.logic.scaling.model.GclNodeInfo;
import jsk.gcl.srv.logic.scaling.schedulers.GclScalingDataGatherScheduler;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.bull.javamelody.MonitoredWithSpring;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import sk.db.relational.spring.services.impl.RdbTransactionManagerImpl;
import sk.exceptions.NotImplementedException;
import sk.services.bytes.IBytes;
import sk.services.free.IFree;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.nodeinfo.INodeInfo;
import sk.services.rescache.IResCache;
import sk.services.time.ITime;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.ids.IdString;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.statics.Ti;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@MonitoredWithSpring
@Log4j2
public class GclStorageFacadeImpl extends RdbTransactionManagerImpl implements GclStorageFacade {
    private @Inject INodeInfo nodeInfo;
    private @Inject IIds ids;
    private @Inject IFree free;
    private @Inject IJson json;
    private @Inject IBytes bytes;
    private @Inject ITime times;
    private @Inject IResCache resCache;

    private @Inject NamedParameterJdbcOperations jdbcQuery;

    private @Inject GclJobGroupJpaRepo jobGroupRepo;
    private QGclJobGroupJpa qjobGroup = QGclJobGroupJpa.gclJobGroupJpa;
    private @Inject GclJobJpaRepo jobRepo;
    private QGclJobJpa qjob = QGclJobJpa.gclJobJpa;
    private @Inject GclJobGroupArchiveJpaRepo jobGroupArchiveRepo;
    private QGclJobGroupArchiveJpa qjobGroupArchive = QGclJobGroupArchiveJpa.gclJobGroupArchiveJpa;
    private @Inject GclNodeJpaRepo nodeRepo;
    private QGclNodeJpa qnode = QGclNodeJpa.gclNodeJpa;
    private @Inject GclNodeArchiveJpaRepo nodeArchiveRepo;
    private QGclNodeArchiveJpa qnodeArchive = QGclNodeArchiveJpa.gclNodeArchiveJpa;

    public GclStorageFacadeImpl(IIds ids) {
        this.ids = ids;
    }

    // ------------------------------------------------
    // region GclJobGroup
    // ------------------------------------------------
    @Override
    public O<GclJobGroup> getJobGroupById(GclJobGroupId jobGroupId) {
        return O.of(jobGroupRepo.findById(jobGroupId).map($ -> $));
    }

    @Override
    public List<GclJobGroup> getFinishedOrFailedJobGroups() {
        return Cc.list(jobGroupRepo.findAll(qjobGroup.jgStatus.in(GclJobStatus.FAIL, GclJobStatus.FINISH)));
    }

    @Override
    public GclJobGroup newGclJobGroup(GclJobGroupDto dto) {
        return new GclJobGroupJpa(dto.getJobGroupId(), prepareTagFromId(dto.getJobGroupId()),
                GclJobStatus.READY,
                new GclJobGroupInnerState(), 0, 0, dto.getJobs().size(), null, null, null);
    }

    @Override
    public void incrementJobGroupSuccess(GclJobGroupId jJgId) {
        privateUpdateJobGroup("gcl/sql/increment_job_group_success.sql", jJgId);
    }

    @Override
    public void incrementJobGroupFail(GclJobGroupId jJgId) {
        privateUpdateJobGroup("gcl/sql/increment_job_group_fail.sql", jJgId);
    }

    @Override
    public long getActiveJobCount() {
        throw new NotImplementedException();
        //todo
    }

    @Override
    public void updateTasksIfLifePingIsOld() {
        //!!!should be set according to GclJobUpdateLifePingScheduler timer (3-4 times that time)
        final ZonedDateTime limitingDate = times.nowZ().minus(1, ChronoUnit.MINUTES);
        jdbcQuery.update(resCache.getResource("gcl/sql/batch_update_tasks_with_old_lifepings.sql.ftl").get(),
                Cc.m("limit_date", Ti.yyyyMMddHHmmss.format(limitingDate)));
    }

    private int privateUpdateJobGroup(String file, GclJobGroupId jJgId) {
        return jdbcQuery.update(resCache.getResource(file).get(), Cc.m("jg_id", jJgId.toString()));
    }

    // endregion

    // ------------------------------------------------
    // region GclJob
    // ------------------------------------------------
    @Override
    public O<GclJob> getJobById(GclJobId jobId) {
        return O.of(jobRepo.findById(jobId).map($ -> $));
    }

    @Override
    public List<GclJob> getJobsRelatedTo(GclJobGroupId jobGroupId) {
        return Cc.list(jobRepo.findAll(qjob.jJgId.eq(jobGroupId)));
    }

    @Override
    public GclJob newGclJob(GclJobGroupId jJgId, GclJobDto<?, ?, ?> job) {
        return new GclJobJpa(job.getJobId(), prepareTagFromId(job.getJobId()), jJgId, null,
                GclJobStatus.READY,
                new GclJobInnerState(job), Ti.minZonedDateTime, null, null, null);
    }

    @Override
    public void updateLifePings(List<GclJobId> activeJobs) {
        int numOfGroups = activeJobs.size() / 15_000 + 1;
        final Map<Integer, List<GclJobId>> groupsToUpdateLifePings =
                numOfGroups > 1
                ? activeJobs.stream().collect(Collectors.groupingBy($ -> $.hashCode() % numOfGroups))
                : Cc.m(0, activeJobs);

        groupsToUpdateLifePings.values().parallelStream().forEach(jobsToUpdatePings -> {
            final String encodedIds = "'" + Cc.join("','", jobsToUpdatePings) + "'";
            final String sql = free.processByText("gcl/sql/batch_update_life_pings.sql.ftl", Cc.m("items", encodedIds), true);
            jdbcQuery.update(sql, Cc.m());
        });
    }

    private void deleteJobsRelatedToGroupId(GclJobGroupId jobGroupId) {
        jdbcQuery.update("delete from gcl.gcl_job where j_jg_id=:j_jg_id", Cc.m("j_jg_id", jobGroupId.toString()));
    }

    // endregion

    // ------------------------------------------------
    // region GclJobGroupArchive
    // ------------------------------------------------
    @Override
    public O<GclJobGroupArchive> getJobGroupArchiveById(GclJobGroupArchiveId jobGroupArchiveId) {
        return O.of(jobGroupArchiveRepo.findById(jobGroupArchiveId).map($ -> $));
    }

    @Override
    @Transactional
    public void moveJobGroupToArchive(GclJobGroup group) {
        final O<byte[]> bytes = this.bytes.zipData(json.to(getJobsRelatedTo(group.getJgId())).getBytes(StandardCharsets.UTF_8));

        deleteJobsRelatedToGroupId(group.getJgId());

        final GclJobGroupArchiveJpa groupArchive = new GclJobGroupArchiveJpa(group.getJgId(),
                group.getJgTag(), group.getJgStatus(),
                group.getJgInnerState(),
                bytes.orElseGet(() -> "Something wrong".getBytes(StandardCharsets.UTF_8))
                , null, null, null);

        jobGroupRepo.delete((GclJobGroupJpa) group);

        saveSingleItem(groupArchive);
    }
    // endregion

    // ------------------------------------------------
    // region GclNode
    // ------------------------------------------------
    @Override
    public O<GclNode> getNodeById(GclNodeId nodeId) {
        return O.of(nodeRepo.findById(nodeId).map($ -> $));
    }

    @Override
    public GclNode getNodeCurrentInfo(GclNodeId nodeId) {
        return getNodeById(nodeId).orElseThrow(() -> new RuntimeException("Unknown nodeId:" + nodeId));
    }

    @Override
    public List<GclNode> getInactiveNodes() {
        return Cc.list(nodeRepo.findAll(qnode.nLifePing.lt(getNodeInactiveTimeLimit())));
    }

    @Override
    public int getActiveNodeCount() {
        return (int) nodeRepo.count(qnode.nLifePing.goe(getNodeInactiveTimeLimit()));
    }

    @Override
    public GclNode ensureNodeIsCreatedAndReturnConfig(GclNodeId nodeId, F0<GclNodeInfo> infoInitializer) {
        final GclNodeJpa toSave = new GclNodeJpa(nodeId, infoInitializer.apply(), times.nowZ(), null, null, null);
        saveSingleItem(toSave);
        return toSave;
    }


    private ZonedDateTime getNodeInactiveTimeLimit() {
        return times.nowZ().minus(4 * GclScalingDataGatherScheduler.NODE_PING_PERIOD_MS, ChronoUnit.MILLIS);
    }

    // endregion

    // ------------------------------------------------
    // region GclNodeArchive
    // ------------------------------------------------
    @Override
    public O<GclNodeArchive> getNodeArchiveById(GclNodeArchiveId nodeArchiveId) {
        return O.of(nodeArchiveRepo.findById(nodeArchiveId).map($ -> $));
    }

    @Override
    @Transactional
    public void archiveNode(GclNodeId archiveNodeId) {
        final GclNode toArchiveNode = getNodeById(archiveNodeId).get();
        final GclNodeArchiveJpa toSave =
                new GclNodeArchiveJpa(new GclNodeArchiveId(toArchiveNode.getNId().getId()), toArchiveNode.getNInnerState(),
                        toArchiveNode.getCreatedAt(), toArchiveNode.getUpdatedAt(), toArchiveNode.getVersion());

        saveSingleItem(toSave);
        nodeRepo.delete((GclNodeJpa) toArchiveNode);
    }
    // endregion


    @Override
    protected void saveSingleItem(Object toSave) {
        if (toSave instanceof GclJobGroupJpa) {
            jobGroupRepo.save((GclJobGroupJpa) toSave);
            return;
        }
        if (toSave instanceof GclJobJpa) {
            jobRepo.save((GclJobJpa) toSave);
            return;
        }
        if (toSave instanceof GclJobGroupArchiveJpa) {
            jobGroupArchiveRepo.save((GclJobGroupArchiveJpa) toSave);
            return;
        }
        if (toSave instanceof GclNodeJpa) {
            nodeRepo.save((GclNodeJpa) toSave);
            return;
        }
        if (toSave instanceof GclNodeArchiveJpa) {
            nodeArchiveRepo.save((GclNodeArchiveJpa) toSave);
            return;
        }

        throw new RuntimeException("Unknown transactional type: " + toSave.getClass());
    }

    String prepareTagFromId(IdString idx) {
        final String id = idx.toString();
        return switch (St.count(id, "-")) {
            case 0 -> ids.tinyHaiku();
            case 1 -> id;
            case 2 -> St.subRL(id, "-");
            default -> {
                final int firstIndex = id.indexOf("-");
                final int secondIndex = id.indexOf("-", firstIndex + 1);
                yield St.ss(id, 0, secondIndex);
            }
        };
    }
}
