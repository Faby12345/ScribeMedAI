
CREATE TABLE rate_limit_window
(
    id                UUID PRIMARY KEY            NOT NULL,
    scope             VARCHAR(255)                NOT NULL,
    identifier        VARCHAR(255)                NOT NULL,
    window_started_at TIMESTAMPTZ                  NOT NULL,
    request_count     INTEGER                     NOT NULL,
    updated_at        TIMESTAMPTZ                  NOT NULL,


    CONSTRAINT uq_scope_identifier UNIQUE (scope, identifier),

    CONSTRAINT chk_request_count_non_negative
        CHECK (request_count >= 0)
);

