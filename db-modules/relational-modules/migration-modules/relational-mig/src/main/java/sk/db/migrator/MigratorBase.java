package sk.db.migrator;

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


import org.flywaydb.core.Flyway;
import sk.services.json.IJson;
import sk.services.json.JGsonImpl;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.Io;
import sk.utils.statics.St;

import java.util.Map;

public class MigratorBase {
    protected IJson json = new JGsonImpl().init();

    public final void migrate(String[] args) {
        doMigration(generateMigratorModel(args));
    }

    protected MigratorModel generateMigratorModel(String[] args) {
        String configFileLocationOrConfigItself = args[0].trim();
        Map<String, String> moreOptions = getMoreOptions(args);

        return Io.getResource(configFileLocationOrConfigItself)
                .or(() -> Io.sRead(configFileLocationOrConfigItself).oString())
                .or(() -> Io.getResource(configFileLocationOrConfigItself))
                .or(() -> configFileLocationOrConfigItself.startsWith("{")
                        ? O.of(configFileLocationOrConfigItself)
                        : O.of("{}"))
                .map($ -> {
                    Map<String, String> from = json.from($, TypeWrap.getMap(String.class, String.class));
                    from.putAll(moreOptions);
                    return json.to(from);
                })
                .map($ -> json.from($, MigratorModel.class))
                .map(this::enrich)
                .orElseThrow();
    }

    protected Map<String, String> getMoreOptions(String[] args) {
        Map<String, String> toRet = Cc.m();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if (i == 0) {
                final boolean firstArgumentIsNotConfig = St.count(arg, "=") == 1 && !arg.startsWith("{");
                if (!firstArgumentIsNotConfig) {
                    continue;
                }
            } else if (St.count(arg, "=") != 1) {
                throw new RuntimeException("Bad format:" + arg);
            }
            String[] split = arg.split("=");
            if (split.length > 2 || split.length < 1) {
                throw new RuntimeException("Bad format:" + arg);
            }
            toRet.put(split[0].trim(), split.length == 1 ? "" : split[1].trim());
        }
        return toRet;
    }

    protected MigratorModel enrich(MigratorModel model) {
        return model;
    }

    protected void doMigration(MigratorModel mm) {
        Flyway flyWay = Flyway.configure()
                .dataSource(mm.connectionString, mm.userName, mm.password)
                .locations(mm.getResourceFolder())
                .table(mm.getMigrationTableName())
                .sqlMigrationPrefix(mm.getFilePrefix())
                .schemas(mm.getSchemaName())
                .load();
        flyWay.migrate();
        flyWay.validate();
    }
}
