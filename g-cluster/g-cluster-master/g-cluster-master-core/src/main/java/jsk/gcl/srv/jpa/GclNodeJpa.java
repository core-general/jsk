
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

import jakarta.persistence.*;
import jsk.gcl.srv.logic.scaling.model.GclNodeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import sk.db.relational.model.JpaWithContextAndCreatedUpdated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gcl_node", schema = "gcl")
public class GclNodeJpa extends JpaWithContextAndCreatedUpdated implements GclNode {
    @Id
    @Column(name = "n_id")
    @Type(value = sk.db.relational.types.UTTextIdToVarchar.class, parameters = {
            @Parameter(name = sk.db.relational.types.UTTextIdToVarchar.param, value = GclNodeId.type)})
    GclNodeId nId;

    @Column(name = "n_inner_state")
    @Type(value = sk.db.relational.types.UTObjectToJsonb.class, parameters = {
            @Parameter(name = sk.db.relational.types.UTObjectToJsonb.param, value = GclNodeInfo.type)})
    GclNodeInfo nInnerState;

    @Column(name = "n_life_ping")
    @Type(value = sk.db.relational.types.UTZdtToTimestamp.class)
    java.time.ZonedDateTime nLifePing;

    @Column(name = "created_at")
    @Type(value = sk.db.relational.types.UTZdtToTimestamp.class)
    java.time.ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @Type(value = sk.db.relational.types.UTZdtToTimestamp.class)
    java.time.ZonedDateTime updatedAt;

    @Version
    java.lang.Long version;

}
