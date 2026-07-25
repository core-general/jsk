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

import org.junit.jupiter.api.Test;
import sk.db.relational.utils.RdbWithChangedPort;
import sk.utils.statics.Cc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JskLandPgTest {
    @Test
    void fixedAndDockerMappedModesConfigureDifferentHostPortPolicies() {
        JskLandPg mapped = new JskLandPg("postgres:16.3");
        var mappedContainer = mapped.createContainer(0);
        assertTrue(mappedContainer.getPortBindings().isEmpty());
        assertTrue(mappedContainer.getExposedPorts().contains(5432));

        RdbWithChangedPort fixedPort = () -> 32123;
        JskLandPg fixed = new JskLandPg(fixedPort, "postgres:16.3");
        var fixedContainer = fixed.createContainer(fixedPort.getPort());
        assertEquals(Cc.l("32123:5432/tcp"), fixedContainer.getPortBindings());
        assertEquals(32123, fixed.getOutsidePort());
    }

    @Test
    void dockerMappedConfigExposesTheLandscapePortThroughRdbWithChangedPort() {
        JskLandPg pg = mock(JskLandPg.class);
        when(pg.getOutsidePort()).thenReturn(32124);

        RdbWithChangedPort port = new JskLandPgConfigDockerMappedPort().RdbWithChangedPort(pg);

        assertEquals(32124, port.getPort());
    }

    @Test
    void prepareDeleteQueryTest() {
        assertEquals("""
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
                       \s
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, JskLandPg.prepareDeleteQuery(Cc.l(), Cc.l()));

        assertEquals("""
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
                        schemaname IN ('abc')
                       \s
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, JskLandPg.prepareDeleteQuery(Cc.l("abc"), Cc.l()));
        assertEquals("""
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
                        schemaname IN ('abc','def')
                       \s
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, JskLandPg.prepareDeleteQuery(Cc.l("abc", "def"), Cc.l()));

        assertEquals("""
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
                        AND tablename <> 'abc'
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, JskLandPg.prepareDeleteQuery(Cc.l(), Cc.l("abc")));
        assertEquals("""
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
                        AND tablename <> 'abc' AND tablename <> 'def'
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, JskLandPg.prepareDeleteQuery(Cc.l(), Cc.l("abc", "def")));

        assertEquals("""
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
                        schemaname IN ('abc')
                        AND tablename <> 'def'
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, JskLandPg.prepareDeleteQuery(Cc.l("abc"), Cc.l("def")));
        assertEquals("""
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
                        schemaname IN ('abc','klm')
                        AND tablename <> 'def' AND tablename <> 'jsk'
                    )
                    LOOP
                        EXECUTE format('TRUNCATE TABLE %I.%I CASCADE;', table_schema, table_name);
                    END LOOP;
                    SET session_replication_role = DEFAULT;
                END;
                $$ LANGUAGE plpgsql;
                """, JskLandPg.prepareDeleteQuery(Cc.l("abc", "klm"), Cc.l("def", "jsk")));
    }
}
