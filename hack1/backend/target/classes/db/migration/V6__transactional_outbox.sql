-- ─────────────────────────────────────────────────────────────
--  V6 — Transactional Outbox
--  Guarantees atomic persistence and reliable WebSocket/STOMP publication
-- ─────────────────────────────────────────────────────────────

CREATE TABLE outbox_events (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    VARCHAR(64)  NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON outbox_events (status, created_at)
    WHERE status = 'PENDING';
