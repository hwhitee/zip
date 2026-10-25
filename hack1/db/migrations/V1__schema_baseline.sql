-- ─────────────────────────────────────────────────────────────
--  V1 — Schema Baseline
--  Establishes common types and the patients table.
-- ─────────────────────────────────────────────────────────────

-- ── Patients ──────────────────────────────────────────────────
CREATE TABLE patients (
    patient_id    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Synthetic MRN — Medical Record Number (never real PHI)
    mrn           VARCHAR(32)  NOT NULL UNIQUE,

    display_name  VARCHAR(128) NOT NULL,
    ward_id       VARCHAR(32)  NOT NULL,   -- e.g. "WARD-4B"
    bed_label     VARCHAR(8)   NOT NULL,   -- e.g. "04"

    admitted_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    discharged_at TIMESTAMPTZ,            -- NULL while active

    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'DISCHARGED', 'TRANSFERRED')),

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Fast lookups by ward (dashboard loads ward-scoped views)
CREATE INDEX idx_patients_ward_status
    ON patients (ward_id, status);

COMMENT ON TABLE  patients              IS 'Synthetic patient registry — no real PHI';
COMMENT ON COLUMN patients.mrn         IS 'Synthetic Medical Record Number';
COMMENT ON COLUMN patients.ward_id     IS 'Ward identifier, e.g. WARD-4B';
COMMENT ON COLUMN patients.bed_label   IS 'Bed position within the ward, e.g. 04';
