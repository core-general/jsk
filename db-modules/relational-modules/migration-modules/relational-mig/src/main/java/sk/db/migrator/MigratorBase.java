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

import java.util.Map;

public class MigratorBase {
    public final void migrate(String[] args) {
        doMigration(generateMigratorModel(args));
    }

    protected MigratorModel generateMigratorModel(String[] args) {
        String configFileLocationOrConfigItself = args[0].trim();
        Map<String, String> moreOptions = getMoreOptions(args);

        IJson js = new JGsonImpl().init();
        return Io.getResource(configFileLocationOrConfigItself)
                .or(() -> Io.sRead(configFileLocationOrConfigItself).oString())
                .or(() -> configFileLocationOrConfigItself.startsWith("{")
                        ? O.of(configFileLocationOrConfigItself)
                        : O.of("{}"))
                .map($ -> {
                    Map<String, String> from = js.from($, TypeWrap.getMap(String.class, String.class));
                    from.putAll(moreOptions);
                    return js.to(from);
                })
                .map($ -> js.from($, MigratorModel.class))
                .orElseThrow();
    }

    protected Map<String, String> getMoreOptions(String[] args) {
        Map<String, String> toRet = Cc.m();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.contains("=")) {
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
