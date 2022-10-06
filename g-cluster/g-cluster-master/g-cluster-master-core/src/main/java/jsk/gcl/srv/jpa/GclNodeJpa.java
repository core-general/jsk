
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import sk.db.relational.model.JpaWithContextAndCreatedUpdated;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gcl_node", schema = "gcl")
public class GclNodeJpa extends JpaWithContextAndCreatedUpdated implements GclNode {
    @Id
    @Column(name = "n_id")
    @Type(type = sk.db.relational.types.UTTextIdToVarchar.type, parameters = {
            @Parameter(name = sk.db.relational.types.UTTextIdToVarchar.param, value = GclNodeId.type)})
    GclNodeId nId;

    @Column(name = "n_inner_state")
    @Type(type = sk.db.relational.types.UTObjectToJsonb.type, parameters = {
            @Parameter(name = sk.db.relational.types.UTObjectToJsonb.param, value = jsk.gcl.srv.scaling.model.GclNodeInfo.type)})
    jsk.gcl.srv.scaling.model.GclNodeInfo nInnerState;

    @Column(name = "n_life_ping")
    @Type(type = sk.db.relational.types.UTZdtToTimestamp.type)
    java.time.ZonedDateTime nLifePing;

    @Column(name = "created_at")
    @Type(type = sk.db.relational.types.UTZdtToTimestamp.type)
    java.time.ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @Type(type = sk.db.relational.types.UTZdtToTimestamp.type)
    java.time.ZonedDateTime updatedAt;

    @Version
    java.lang.Long version;

}
