package sk.db.kv;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import lombok.*;
import org.hibernate.annotations.Type;
import sk.db.relational.model.JpaWithContextAndCreatedUpdated;
import sk.db.relational.types.UTZdtToTimestamp;

import javax.persistence.*;
import java.time.ZonedDateTime;

@EqualsAndHashCode(callSuper = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_general_purpose_kv")
public class JpaKVItem extends JpaWithContextAndCreatedUpdated {
    @EmbeddedId
    KVItemId id;

    @Column(name = "value")
    String value;

    @Column(name = "lock_date")
    Long lockDate;

    @Column(name = "created_at")
    @Type(type = UTZdtToTimestamp.type)
    ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @Type(type = UTZdtToTimestamp.type)
    ZonedDateTime updatedAt;

    @Version
    Long version;
}
