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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import sk.db.relational.types.UTZdtToBigInt;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_configuration")
public class JpaAppProperties {
    @EmbeddedId
    Id id;
    @Column(name = "value")
    String value;
    @Column(name = "property_date")
    @Type(type = UTZdtToBigInt.type)
    ZonedDateTime propertyDate;
    @Column(name = "description")
    String description;

    @Version
    Long version;

    @Embeddable
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {
        @Column(name = "property_category")
        String propertyCategory;
        @Column(name = "property_id")
        String propertyId;
    }
}
