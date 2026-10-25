-- ─────────────────────────────────────────────────────────────
--  V3 — Inference Results
--  Stores the output of the Python FastAPI scoring engine
--  for every processed sensor window.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE inference_results (
    result_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    window_id       UUID         NOT NULL
        REFERENCES sensor_windows (window_id) ON DELETE CASCADE,

    patient_id      UUID         NOT NULL
        REFERENCES patients (patient_id) ON DELETE CASCADE,

    computed_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    latency_ms      INTEGER      CHECK (latency_ms >= 0),

    -- Core risk output
    risk_score      NUMERIC(6,5) NOT NULL
        CHECK (risk_score >= 0.0 AND risk_score <= 1.0),

    risk_level      VARCHAR(16)  NOT NULL
        CHECK (risk_level IN ('LOW', 'ELEVATED', 'HIGH', 'CRITICAL')),

    -- JSON array of { feature_name, weight, value } objects
    -- Used to render contributing-factors breakdown in the UI
    contributing_factors  JSONB,

    -- Optional Gemini-generated natural language explanation
    -- NULL when GEMINI_API_KEY is absent or call failed
    gemini_explanation    TEXT,

    -- Model provenance
    model_version   VARCHAR(32)  NOT NULL,         -- e.g. "v0.1.0-heuristic"
    model_type      VARCHAR(16)  NOT NULL DEFAULT 'HEURISTIC'
        CHECK (model_type IN ('HEURISTIC', 'ML', 'SKIPPED')),

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Most common query: latest result per patient for the dashboard
CREATE INDEX idx_ir_patient_computed
    ON inference_results (patient_id, computed_at DESC);

-- Filter by risk level to find active high-risk patients
CREATE INDEX idx_ir_risk_level
    ON inference_results (risk_level, computed_at DESC)
    WHERE risk_level IN ('HIGH', 'CRITICAL');

COMMENT ON TABLE  inference_results                     IS 'Scoring output from FastAPI inference service per sensor window';
COMMENT ON COLUMN inference_results.risk_score          IS 'Normalised risk score 0.0 (safe) – 1.0 (critical)';
COMMENT ON COLUMN inference_results.contributing_factors IS 'JSON array [{feature_name, weight, value}] for explainability';
COMMENT ON COLUMN inference_results.gemini_explanation  IS 'Plain-language explanation from Gemini; NULL if not requested or failed';
