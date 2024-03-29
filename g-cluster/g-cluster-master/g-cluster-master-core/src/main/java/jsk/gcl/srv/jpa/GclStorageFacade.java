
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

import jsk.gcl.srv.logic.jobs.storage.GclJobArchiveStorage;
import jsk.gcl.srv.logic.jobs.storage.GclJobStorage;
import jsk.gcl.srv.logic.scaling.storage.GclNodeArchiveStorage;
import jsk.gcl.srv.logic.scaling.storage.GclNodeStorage;
import sk.db.relational.spring.services.RdbTransactionManager;

public interface GclStorageFacade
        extends RdbTransactionManager,
                GclNodeStorage,
                GclJobArchiveStorage,
                GclJobStorage,
                GclNodeArchiveStorage {}
