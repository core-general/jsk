---
-- #%L
-- Swiss Knife
-- %%
-- Copyright (C) 2019 - 2022 Core General
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--      http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
---
CREATE TABLE gcl_job_group
(
    jg_id          TEXT      NOT NULL PRIMARY KEY,
    jg_tag         TEXT      NOT NULL,

    jg_status      TEXT      NOT NULL,
    jg_inner_state JSONB     NOT NULL,

    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    version        BIGINT    NOT NULL
);
-- @enum gcl_job_group jg_status jsk.gcl.srv.scaling.model.GclJobStatus
-- @jsonb gcl_job_group jg_inner_state jsk.gcl.srv.scaling.model.GclJobGroupInnerState
CREATE INDEX ON gcl_job_group USING BTREE (jg_tag);
CREATE INDEX ON gcl_job_group USING BTREE (jg_status);

CREATE TABLE gcl_job
(
    j_id          TEXT      NOT NULL PRIMARY KEY,
    j_tag         TEXT      NOT NULL,

    j_jg_id       UUID      NOT NULL,

    j_status      TEXT      NOT NULL,
    j_inner_state JSONB     NOT NULL,

    j_life_ping   TIMESTAMP NOT NULL,

    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    version       BIGINT    NOT NULL,
    FOREIGN KEY (j_jg_id) REFERENCES gcl_job_group (jg_id) ON DELETE CASCADE
);
-- @enum gcl_job j_status jsk.gcl.srv.scaling.model.GclJobStatus
-- @relationHere gcl_job j_jg_id gcl_job_group
-- @jsonb gcl_job j_inner_state jsk.gcl.srv.scaling.model.GclJobInnerState
CREATE INDEX ON gcl_job USING BTREE (j_tag);
CREATE INDEX ON gcl_job USING BTREE (j_jg_id);
CREATE INDEX ON gcl_job USING BTREE (j_life_ping);


CREATE TABLE gcl_job_group_archive
(
    jg_id          TEXT      NOT NULL PRIMARY KEY,
    tag            TEXT      NOT NULL,

    jg_status      TEXT      NOT NULL,
    jg_inner_state JSONB     NOT NULL,

    jg_zipped_jobs bytea     NOT NULL,

    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    version        BIGINT    NOT NULL
);
-- @enum gcl_job_group_archive jg_status jsk.gcl.srv.scaling.model.GclJobStatus
-- @jsonb gcl_job_group_archive jg_inner_state jsk.gcl.srv.scaling.model.GclJobGroupInnerState
CREATE INDEX ON gcl_job_group_archive USING BTREE (created_at);
CREATE INDEX ON gcl_job_group_archive USING BTREE (tag);

CREATE TABLE gcl_node
(
    n_id          TEXT      NOT NULL PRIMARY KEY,

    n_inner_state JSONB     NOT NULL,

    n_life_ping   TIMESTAMP NOT NULL,

    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    version       BIGINT    NOT NULL
);
-- @jsonb gcl_node n_inner_state jsk.gcl.srv.scaling.model.GclNodeInfo
CREATE INDEX ON gcl_node USING BTREE (n_life_ping);
CREATE INDEX ON gcl_node USING BTREE (created_at);

CREATE TABLE gcl_node_archive
(
    n_id          TEXT      NOT NULL PRIMARY KEY,

    n_inner_state JSONB     NOT NULL,

    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    version       BIGINT    NOT NULL
);
CREATE INDEX ON gcl_node_archive USING BTREE (created_at);
-- @jsonb gcl_node_archive n_inner_state jsk.gcl.srv.scaling.model.GclNodeInfo
