-- ─────────────────────────────────────────────────────────────
--  V4 — Safety Events
--  Stateful clinical alert records. Created automatically when
--  an inference result reaches HIGH or CRITICAL level.
--
--  Lifecycle FSM:
--    DETECTED → ACKNOWLEDGED → INVESTIGATING → RESOLVED
--                                           → ESCALATED → RESOLVED
--    DETECTED → DISMISSED  (false-positive override by clinician)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE safety_events (
    event_id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    patient_id          UUID         NOT NULL
        REFERENCES patients (patient_id) ON DELETE CASCADE,

    ward_id             VARCHAR(32)  NOT NULL,

    -- The specific inference result that triggered this event
    triggering_result_id UUID
        REFERENCES inference_results (result_id) ON DELETE SET NULL,

    severity            VARCHAR(16)  NOT NULL
        CHECK (severity IN ('LOW', 'ELEVATED', 'HIGH', 'CRITICAL')),

    status              VARCHAR(20)  NOT NULL DEFAULT 'DETECTED'
        CHECK (status IN (
            'DETECTED',
            'ACKNOWLEDGED',
            'INVESTIGATING',
            'ESCALATED',
            'RESOLVED',
            'DISMISSED'
        )),

    -- ── Timestamps for each lifecycle transition ───────────────
    detected_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    acknowledged_at     TIMESTAMPTZ,          -- NULL until ACK
    resolved_at         TIMESTAMPTZ,          -- NULL until closed

    -- ── Assignment ────────────────────────────────────────────
    assigned_to         VARCHAR(128),         -- nurse / clinician ID or name

    -- ── Audit log (append-only array of note objects) ─────────
    -- Each note: { "ts": "ISO8601", "author": "...", "text": "..." }
    notes               JSONB        NOT NULL DEFAULT '[]'::jsonb,

    -- Milliseconds after detection before auto-escalation fires
    -- NULL = manual escalation only
    auto_escalate_after_ms  INTEGER,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Dashboard: open events for a ward, newest first
CREATE INDEX idx_se_ward_status
    ON safety_events (ward_id, status, detected_at DESC);

-- Patient timeline view
CREATE INDEX idx_se_patient_detected
    ON safety_events (patient_id, detected_at DESC);

-- Quickly find events still awaiting acknowledgement (for auto-escalation job)
CREATE INDEX idx_se_detected_unacked
    ON safety_events (detected_at)
    WHERE status = 'DETECTED';

COMMENT ON TABLE  safety_events                       IS 'Clinical safety alerts with full state-machine lifecycle';
COMMENT ON COLUMN safety_events.notes                 IS 'Append-only audit log: [{ts, author, text}]';
COMMENT ON COLUMN safety_events.auto_escalate_after_ms IS 'Auto-escalation window in ms; NULL = never auto-escalate';
