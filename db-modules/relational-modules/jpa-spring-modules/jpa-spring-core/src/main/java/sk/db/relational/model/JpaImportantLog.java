package sk.db.relational.model;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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
import sk.db.relational.types.UTStringToJsonb;
import sk.db.relational.types.UTUuidIdToUuid;
import sk.db.relational.types.UTZdtToTimestamp;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "important_log")
public class JpaImportantLog extends JpaWithContextAndCreatedUpdated {
    @Id
    @Column(name = "id")
    @Type(type = UTUuidIdToUuid.type, parameters = {@Parameter(name = UTUuidIdToUuid.param, value = ImportantLogId.type)})
    ImportantLogId id;

    @Column(name = "category")
    String category;

    @Column(name = "type")
    String type;

    @Column(name = "info")
    @Type(type = UTStringToJsonb.type)
    String info;

    @Version
    @Column(name = "counter")
    Long counter;

    @Column(name = "created_at")
    @Type(type = UTZdtToTimestamp.type)
    ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @Type(type = UTZdtToTimestamp.type)
    ZonedDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        ZonedDateTime now = ctx.times().nowZ();
        if (getCreatedAt() == null) {
            setCreatedAt(now);
        }
        setUpdatedAt(now);
    }
}

