---
-- #%L
-- Swiss Knife
-- %%
-- Copyright (C) 2019 - 2020 Core General
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
CREATE TABLE abc_t1
(
    id         TEXT   NOT NULL PRIMARY KEY,

    info       JSONB  NOT NULL,

    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version    BIGINT NOT NULL
);

-- @jsonb abc_t1 info com.example.SomeInfoClass

CREATE TABLE abc_t2
(
    id         UUID   NOT NULL primary Key,

    type       TEXT   not null,
    t1_id      TEXT   NOT null,
    t0_id      UUID   NOT NULL,
    t2_id      bytea  NOT NULL,

    zzz        timestamp,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version    BIGINT NOT NULL
);

-- @enum abc_t2 type com.example.SomeEnum
-- @relationHere abc_t2 t1_id abc_t1
-- @relationOutside abc_t2 t0_id com.example.T0Id com.example.T0Jpa
