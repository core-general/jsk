
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
import jsk.gcl.srv.scaling.model.GclJobGroupInnerState;
import jsk.gcl.srv.scaling.model.GclJobInnerState;
import jsk.gcl.srv.scaling.model.GclJobStatus;
import jsk.gcl.srv.scaling.model.GclNodeInfo;
import jsk.gcl.srv.scaling.schedulers.GclScalingDataGatherScheduler;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.bull.javamelody.MonitoredWithSpring;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import sk.db.relational.spring.services.impl.RdbTransactionManagerImpl;
import sk.services.bytes.IBytes;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.nodeinfo.INodeInfo;
import sk.services.time.ITime;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.statics.Ti;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@NoArgsConstructor
@MonitoredWithSpring
@Log4j2
public class GclStorageFacadeImpl extends RdbTransactionManagerImpl implements GclStorageFacade {
    private @Inject INodeInfo nodeInfo;
    private @Inject IIds ids;
    private @Inject IJson json;
    private @Inject IBytes bytes;
    private @Inject ITime times;

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
    public GclJobGroup newGclJobGroup(GclJobGroupDto dto) {
        return new GclJobGroupJpa(new GclJobGroupId(dto.getJobGroupId()), prepareTagFromId(dto.getJobGroupId()),
                GclJobStatus.READY,
                new GclJobGroupInnerState(dto.getJobs().size()), null, null, null);
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

    private void deleteJobsRelatedToGroupId(GclJobGroupId jobGroupId) {
        jdbcQuery.update("delete from gcl.gcl_job where j_jg_id=:j_jg_id", Cc.m("j_jg_id", jobGroupId.toString()));
    }

    @Override
    public GclJob newGclJob(GclJobGroupId jJgId, GclJobDto<?, ?> job) {
        return new GclJobJpa(new GclJobId(job.getJobId()), prepareTagFromId(job.getJobId()), jJgId, null,
                GclJobStatus.READY,
                new GclJobInnerState(job), Ti.minZonedDateTime, null, null, null);
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

        final GclJobGroupArchiveJpa groupArchive = new GclJobGroupArchiveJpa(new GclJobGroupArchiveId(group.getJgId()),
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
    public long getActiveNodeCount() {
        return nodeRepo.count(qnode.nLifePing.goe(getNodeInactiveTimeLimit()));
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

    String prepareTagFromId(String id) {
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
