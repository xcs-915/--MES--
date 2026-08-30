CREATE TABLE mes_outbox_message (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    event_id NVARCHAR(64) NOT NULL,
    aggregate_type NVARCHAR(80) NOT NULL,
    aggregate_id NVARCHAR(64) NOT NULL,
    event_type NVARCHAR(120) NOT NULL,
    payload NVARCHAR(MAX) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME2,
    published_at DATETIME2,
    last_error NVARCHAR(2000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mes_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX ix_mes_outbox_pending ON mes_outbox_message(status, next_attempt_at, created_at);

