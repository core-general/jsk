package sk.db.relational.spring.services.impl;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

@Entity
@Table(name = "retry_record")
public class RetryRecord {
    @Id public String id;
    @Column(name = "natural_key", nullable = false, unique = true) public String key;
    @Version public Long version;
    public int value;

    public RetryRecord() {}
    public RetryRecord(String id, String key) { this.id = id; this.key = key; }
}
