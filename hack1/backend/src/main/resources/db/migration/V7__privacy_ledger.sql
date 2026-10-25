-- ─────────────────────────────────────────────────────────────
--  V7 — Auditable Privacy Ledger
--  Cryptographic audit log for privacy preservation verification
-- ─────────────────────────────────────────────────────────────

CREATE TABLE privacy_ledger (
    entry_id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    window_id               UUID         NOT NULL,
    patient_token           VARCHAR(64)  NOT NULL,
    event_type              VARCHAR(64)  NOT NULL,
    canonical_sha256        VARCHAR(64)  NOT NULL,
    prev_hash               VARCHAR(64)  NOT NULL,
    data_minimization_note  TEXT         NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_privacy_window ON privacy_ledger (window_id);
CREATE INDEX idx_privacy_created ON privacy_ledger (created_at DESC);
