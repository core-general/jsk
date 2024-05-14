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

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import sk.db.relational.utils.RdbProperties;
import sk.db.relational.utils.RdbUtil;
import sk.db.relational.utils.RdbWithChangedPort;
import sk.test.land.core.JskLand;
import sk.test.land.testcontainers.JskLandContainer;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.St;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JskLandPg extends JskLandContainer<PostgreSQLContainer<?>> {
    @NotNull private final RdbWithChangedPort portProvider;
    protected final String dockerImgName;

    public JskLandPg(RdbWithChangedPort outsidePort, String dockerImgName) {
        super(outsidePort.getPort());
        portProvider = outsidePort;
        this.dockerImgName = dockerImgName;
    }

    private final ConcurrentHashMap<String, DataSource> ds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, NamedParameterJdbcOperations> sql = new ConcurrentHashMap<>();

    public NamedParameterJdbcOperations getSql() {
        return getSql(getContainer().getDatabaseName());
    }

    public NamedParameterJdbcOperations getSql(String database) {
        return sql.computeIfAbsent(database, key -> new NamedParameterJdbcTemplate(getOrCreateDS(database)));
    }

    public void clearAllNonPostgresTablesIn(String database) {
        getSql(database).update("""
                DO $$
                DECLARE
                    table_schema text;
                    table_name text;
                BEGIN
                    SET session_replication_role = replica;
                    FOR table_schema, table_name IN (
                        SELECT schemaname, tablename
                        FROM pg_tables
                        WHERE
                        schemaname NOT IN ('information_schema', 'pg_catalog')
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, Cc.m());
    }

    @Override
    protected PostgreSQLContainer createContainer(int port) {
        PostgreSQLContainer<?> selfPostgreSQLContainer = new PostgreSQLContainer<>(dockerImgName)
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")
                .withStartupTimeoutSeconds(150);
        selfPostgreSQLContainer.setPortBindings(Cc.l(port + ":5432"));
        return selfPostgreSQLContainer;
    }

    @Override
    public Class<? extends JskLand> getId() {
        return JskLandPg.class;
    }


    @NotNull
    private DataSource getOrCreateDS(String database) {
        return ds.computeIfAbsent(database, (___) -> createDataSource(database));
    }

    private DataSource createDataSource(String dbName) {
        if (!Fu.equalIgnoreCase(getContainer().getDatabaseName(), dbName)) {
            //trying to create database if not exist
            getSql().update("create database " + dbName, Cc.m());
        }

        return RdbUtil.createDatasource(new RdbProperties() {
            @Override
            public String getDriver() {
                return "org.postgresql.Driver";
            }

            @Override
            public String getUrl() {
                return St.subRL(getContainer().getJdbcUrl(), "/") + "/" + dbName;
            }

            @Override
            public String getUser() {
                return getContainer().getUsername();
            }

            @Override
            public String getPass() {
                return getContainer().getPassword();
            }

            @Override
            public int getMaxPoolSize() {
                return 5;
            }

            @Override
            public String[] getJpaPackages() {
                return new String[0];
            }

            @Override
            public Boolean getShowInfo() {
                return false;
            }

            @Override
            public String getDialect() {
                return "";
            }
        }, Optional.of(portProvider));
    }


}
