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
    id         UUID   NOT NULL PRIMARY KEY,

    type       TEXT   NOT NULL,
    t1_id      TEXT   NOT NULL,
    t0_id      UUID   NOT NULL,

    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version    BIGINT NOT NULL
);

-- @enum abc_t2 type com.example.SomeEnum
-- @relationHere abc_t2 t1_id abc_t1
-- @relationOutside abc_t2 t0_id com.example.T0Id com.example.T0Jpa