-- ─────────────────────────────────────────────────────────────
--  V8 — Allow SKIPPED Model Type
--  Ensures inference_results allows SKIPPED when inference service fails
-- ─────────────────────────────────────────────────────────────

ALTER TABLE inference_results DROP CONSTRAINT IF EXISTS inference_results_model_type_check;
ALTER TABLE inference_results ADD CONSTRAINT inference_results_model_type_check
    CHECK (model_type IN ('HEURISTIC', 'ML', 'SKIPPED'));
