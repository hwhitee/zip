-- ─────────────────────────────────────────────────────────────
--  V5 — Staff and Beds
--  Represents clinical ward beds and available nursing staff
-- ─────────────────────────────────────────────────────────────

CREATE TABLE beds (
    bed_id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    ward_id             VARCHAR(32)  NOT NULL,
    bed_label           VARCHAR(8)   NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'OCCUPIED'
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'CLEANING', 'MAINTENANCE')),
    current_patient_id  UUID         REFERENCES patients (patient_id) ON DELETE SET NULL,
    x_coord             DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    y_coord             DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (ward_id, bed_label)
);

CREATE INDEX idx_beds_ward_status ON beds (ward_id, status);

CREATE TABLE staff (
    staff_id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(128) NOT NULL,
    role                    VARCHAR(32)  NOT NULL DEFAULT 'STAFF_NURSE'
        CHECK (role IN ('STAFF_NURSE', 'CHARGE_NURSE', 'NURSE_PRACTITIONER', 'PHYSICIAN')),
    on_shift                BOOLEAN      NOT NULL DEFAULT TRUE,
    available               BOOLEAN      NOT NULL DEFAULT TRUE,
    primary_ward_id         VARCHAR(32)  NOT NULL,
    eligible_wards          VARCHAR(128) NOT NULL DEFAULT 'WARD-4B',
    active_assignments_count INTEGER     NOT NULL DEFAULT 0,
    current_x               DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    current_y               DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_staff_ward_shift ON staff (primary_ward_id, on_shift, available);
