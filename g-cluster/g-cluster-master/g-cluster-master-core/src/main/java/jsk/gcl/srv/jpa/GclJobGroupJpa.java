
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

import jsk.gcl.cli.model.GclJobGroupId;
import jsk.gcl.srv.logic.jobs.model.GclJobGroupInnerState;
import jsk.gcl.srv.logic.jobs.model.GclJobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import sk.db.relational.model.JpaWithContextAndCreatedUpdated;
import sk.db.relational.types.UTEnumToString;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gcl_job_group", schema = "gcl")
public class GclJobGroupJpa extends JpaWithContextAndCreatedUpdated implements GclJobGroup {
    @Id
    @Column(name = "jg_id")
    @Type(type = sk.db.relational.types.UTTextIdToVarchar.type, parameters = {
            @Parameter(name = sk.db.relational.types.UTTextIdToVarchar.param, value = GclJobGroupId.type)})
    GclJobGroupId jgId;

    @Column(name = "jg_tag")
    java.lang.String jgTag;

    @Column(name = "jg_status")
    @Type(type = UTEnumToString.type, parameters = {
            @Parameter(name = UTEnumToString.param, value = GclJobStatus.type)})
    GclJobStatus jgStatus;

    @Column(name = "jg_inner_state")
    @Type(type = sk.db.relational.types.UTObjectToJsonb.type, parameters = {
            @Parameter(name = sk.db.relational.types.UTObjectToJsonb.param, value =
                    GclJobGroupInnerState.type)})
    GclJobGroupInnerState jgInnerState;

    @Column(name = "jg_num_of_success_tasks")
    int numOfSuccessTasks;

    @Column(name = "jg_num_of_fail_tasks")
    int numOfFailTasks;

    @Column(name = "jg_num_of_overall_tasks")
    int numOfOverallTasks;

    @Column(name = "created_at")
    @Type(type = sk.db.relational.types.UTZdtToTimestamp.type)
    java.time.ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @Type(type = sk.db.relational.types.UTZdtToTimestamp.type)
    java.time.ZonedDateTime updatedAt;

    @Version
    java.lang.Long version;

}
