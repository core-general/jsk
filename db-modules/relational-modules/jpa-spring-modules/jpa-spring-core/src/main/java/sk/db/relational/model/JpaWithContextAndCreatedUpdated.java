package sk.db.relational.model;

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

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Setter
@Getter
@NoArgsConstructor
@MappedSuperclass
public abstract class JpaWithContextAndCreatedUpdated extends JpaWithContext {
    @PrePersist
    @PreUpdate
    public void prePersist() {
        ZonedDateTime now = ctx.times().nowZ();
        if (getCreatedAt() == null) {
            setCreatedAt(now);
        }
        setUpdatedAt(now);
    }

    public abstract void setUpdatedAt(ZonedDateTime now);

    public abstract void setCreatedAt(ZonedDateTime now);

    public abstract ZonedDateTime getCreatedAt();
}
