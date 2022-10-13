package jsk.gcl.migrator;

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

import sk.db.migrator.MigratorBase;
import sk.db.migrator.MigratorModel;

public class GclMigratorMain {
    public static void main(String[] args) {
        new GclCommonMigrator().migrate(args);
    }

    private static class GclCommonMigrator extends MigratorBase {
        @Override
        protected MigratorModel enrich(MigratorModel model) {
            model.setResourceFolder("classpath:g-cluster-common-migration");
            model.setMigrationTableName("_gcl_migrations");
            model.setFilePrefix("gcl_");
            model.setSchemaName("gcl_");
            return model;
        }
    }
}
