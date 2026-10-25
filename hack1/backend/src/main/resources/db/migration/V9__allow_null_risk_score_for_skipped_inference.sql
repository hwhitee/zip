-- ─────────────────────────────────────────────────────────────
--  V9 — Allow NULL Risk Score for SKIPPED Inference
--  Ensures that when inference fails or is skipped, the system does not
--  fabricate a fake numeric score, but represents degraded state explicitly.
-- ─────────────────────────────────────────────────────────────

ALTER TABLE inference_results ALTER COLUMN risk_score DROP NOT NULL;
ALTER TABLE inference_results DROP CONSTRAINT IF EXISTS inference_results_risk_score_check;
ALTER TABLE inference_results ADD CONSTRAINT inference_results_risk_score_check
    CHECK (risk_score IS NULL OR (risk_score >= 0.0 AND risk_score <= 1.0));
