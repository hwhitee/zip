-- ─────────────────────────────────────────────────────────────
--  V2 — Sensor Windows
--  Stores the raw 5-minute ambient sensor payloads per patient.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE sensor_windows (
    window_id       UUID         PRIMARY KEY,   -- supplied by producer (UUID v4)

    patient_id      UUID         NOT NULL
        REFERENCES patients (patient_id) ON DELETE CASCADE,

    start_epoch     TIMESTAMPTZ  NOT NULL,      -- inclusive window start
    end_epoch       TIMESTAMPTZ  NOT NULL,      -- exclusive window end (start + 5 min)

    sampling_hz     NUMERIC(5,2) NOT NULL DEFAULT 1.0
        CHECK (sampling_hz > 0 AND sampling_hz <= 100),

    signal_quality  VARCHAR(16)  NOT NULL DEFAULT 'GOOD'
        CHECK (signal_quality IN ('GOOD', 'DEGRADED', 'LOST')),

    -- Full SensorWindow JSON (7 sensor arrays + metadata)
    -- Stored as JSONB for indexing capability and schema flexibility
    payload         JSONB        NOT NULL,

    device_id       VARCHAR(64),              -- originating sensor device
    firmware_ver    VARCHAR(32),

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Primary query pattern: latest N windows for a patient
CREATE INDEX idx_sw_patient_start
    ON sensor_windows (patient_id, start_epoch DESC);

-- Allow filtering by signal quality (skip LOST windows in analysis)
CREATE INDEX idx_sw_quality
    ON sensor_windows (signal_quality)
    WHERE signal_quality != 'GOOD';   -- partial index, small footprint

-- Prevent overlapping windows for the same patient
-- (informational — enforced at application layer too)
COMMENT ON TABLE  sensor_windows                IS '5-minute ambient sensor windows per patient';
COMMENT ON COLUMN sensor_windows.payload        IS 'Full SensorWindow JSON including all 7 sensor arrays';
COMMENT ON COLUMN sensor_windows.window_id      IS 'Producer-assigned UUID v4; idempotent on re-submission';
