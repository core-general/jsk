package sk.test.land.testcontainers.pg;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import sk.services.ICoreServices;
import sk.test.land.core.JskLand;
import sk.test.land.core.mixins.JskLandEmptyStateMixin;
import sk.test.land.core.mixins.JskLandStateChangerMixin;
import sk.utils.functional.O;

@Slf4j
public class JskLandPgWithData extends JskLand
        implements JskLandEmptyStateMixin, JskLandStateChangerMixin<JskLandPgStateChangerAccumulator> {
    private final ICoreServices core;
    @Getter private final JskLandPg pgLand;
    private final String databaseName;

    @Getter
    protected volatile NamedParameterJdbcOperations sql;

    public JskLandPgWithData(ICoreServices core, JskLandPg pgLand, String databaseName) {
        this.core = core;
        this.pgLand = pgLand;
        this.databaseName = databaseName;
    }

    @Override
    public Class<? extends JskLand> getId() {
        return JskLandPgWithData.class;
    }

    @Override
    protected void doInit() throws Exception {
        sql = pgLand.getSql(databaseName);
    }

    @Override
    protected void doShutdown() throws Exception {
        pgLand.stop();
    }

    @Override
    public void toEmptyState() {
        pgLand.clearAllNonPostgresTablesIn(databaseName);
    }

    @Override
    public void toMaybeState(O<JskLandPgStateChangerAccumulator> state) {
        state.ifPresent($ -> $.getStateChangers().parallelStream().forEach($$ -> $$.changePgState(pgLand, databaseName)));
    }

    @Override
    public Class<JskLandPgStateChangerAccumulator> getStateCls() {
        return JskLandPgStateChangerAccumulator.class;
    }
}
