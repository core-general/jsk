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

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.spring.services.CoreServices;
import sk.utils.javafixes.FlushableDetachable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class JpaWithContext implements FlushableDetachable {
    protected transient @Transient CoreServices ctx;
    protected transient @Transient boolean flush;
    protected transient @Transient boolean detach;
}
