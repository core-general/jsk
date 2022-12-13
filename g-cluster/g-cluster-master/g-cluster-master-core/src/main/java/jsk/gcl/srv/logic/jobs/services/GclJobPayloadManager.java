package jsk.gcl.srv.logic.jobs.services;

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

import jsk.gcl.cli.model.GclJobType;
import sk.utils.functional.F1;

/**
 * Manager which actually knows how to execute a particular task.
 * Should be implemented in a module which represent target cluster which needs to be established
 * Should be only the part of the cluster master, cluster client doesn't know how to execute task.
 */
public interface GclJobPayloadManager {
    <PARAM_CLS, OUT_CLS, T extends GclJobType<PARAM_CLS, OUT_CLS>> F1<PARAM_CLS, OUT_CLS> getJobRunner(T jobType);
}
