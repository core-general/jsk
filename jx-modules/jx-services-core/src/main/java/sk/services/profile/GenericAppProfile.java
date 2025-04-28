package sk.services.profile;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenericAppProfile implements IAppProfileType {
    /** local run */
    LOC(true, false),
    /** functional tests */
    FUT(true, false),
    /** dev */
    DEV(false, false),
    /** test */
    TST(false, false),
    /** stage */
    STG(false, false),
    /** prod */
    PRD(false, true);

    public static final String LOCAL = "loc";

    final boolean forDefaultTesting;
    final boolean forProductionUsage;
}
