package sk.db.relational.utils;

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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sk.utils.statics.St;

import javax.sql.DataSource;
import java.util.Optional;

public final class RdbUtil {
    public static DataSource createDatasource(RdbProperties conf, Optional<RdbWithChangedPort> changedPort) {
        HikariConfig cpds = new HikariConfig();
        try {
            cpds.setDriverClassName(conf.getDriver());
        } catch (Exception e) {
            System.err.println("Cannot find driver:" + conf.getDriver());
            System.exit(1);
        }
        cpds.setJdbcUrl(changedPort.map($ -> changePortForUrl(conf.getUrl(), $.getPort()))
                .orElse(conf.getUrl()));
        cpds.setUsername(conf.getUser());
        cpds.setPassword(conf.getPass());
        cpds.setMaximumPoolSize(conf.getMaxPoolSize());
        cpds.setConnectionTimeout(30_000);

        cpds.addDataSourceProperty("stringtype", "unspecified");

        return new HikariDataSource(cpds);
    }

    public static String changePortForUrl(String url, int newPort) {
        String urlWithPort = St.subRF(St.subLF(url, "://"), "/");
        String template = url.replace(urlWithPort, "%s");
        return urlWithPort.contains(":")
               ? template.formatted(St.subRF(urlWithPort, ":") + ":" + newPort)
               : template.formatted(urlWithPort + ":" + newPort);
    }
}
