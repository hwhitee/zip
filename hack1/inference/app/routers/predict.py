"""
/predict, /health, /model/info router.
"""

from __future__ import annotations

import time
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from app.models.predictor import MODEL_VERSION, THRESHOLDS, classify_risk, score_window
from app.models.schemas import (
    ApiError,
    HealthResponse,
    InferenceResult,
    ModelInfoResponse,
    ModelType,
    RiskLevel,
    RiskThresholds,
    SensorWindow,
)
from app.services.gemini_explain import generate_explanation

router = APIRouter()

# Track startup time for uptime reporting
_START_TIME: float = time.monotonic()


# ─────────────────────────────────────────────────────────────────────────────
#  POST /predict
# ─────────────────────────────────────────────────────────────────────────────


@router.post(
    "/predict",
    response_model=InferenceResult,
    response_model_by_alias=False,
    summary="Score a sensor window",
    description=(
        "Accepts a 5-minute ambient sensor window and returns a heuristic "
        "demonstration risk score.  **Not a calibrated clinical probability.** "
        "Not clinically validated."
    ),
    responses={
        422: {"description": "Validation error – malformed or out-of-bounds input"},
    },
)
async def predict(window: SensorWindow, request: Request) -> InferenceResult:
    t0 = time.monotonic()

    result = score_window(window)
    risk_level = classify_risk(result.risk_score, THRESHOLDS)

    # Optional Gemini explanation (never blocks on failure)
    explanation: str | None = None
    if risk_level in (RiskLevel.HIGH, RiskLevel.CRITICAL):
        explanation = await generate_explanation(
            risk_level=risk_level,
            risk_score=result.risk_score,
            factors=result.factors,
            missing_modalities=result.missing_modalities,
            warnings=result.warnings,
        )

    latency_ms = round((time.monotonic() - t0) * 1000)
    now_ms = int(datetime.now(tz=timezone.utc).timestamp() * 1000)

    return InferenceResult(
        result_id=uuid.uuid4(),
        window_id=window.window_id,
        patient_id=window.patient_id,
        computed_at_ms=now_ms,
        latency_ms=latency_ms,
        risk_score=result.risk_score,
        risk_level=risk_level,
        risk_thresholds=THRESHOLDS,
        contributing_factors=result.factors,
        uncertainty=result.uncertainty,
        missing_modalities=result.missing_modalities,
        warnings=result.warnings,
        scenario_hint=result.scenario_hint,
        gemini_explanation=explanation,
        model_version=MODEL_VERSION,
        model_type=result.model_type,
    )


# ─────────────────────────────────────────────────────────────────────────────
#  GET /health
# ─────────────────────────────────────────────────────────────────────────────


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Liveness check",
)
async def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        model_version=MODEL_VERSION,
        uptime_seconds=round(time.monotonic() - _START_TIME, 2),
    )


# ─────────────────────────────────────────────────────────────────────────────
#  GET /model/info
# ─────────────────────────────────────────────────────────────────────────────


@router.get(
    "/model/info",
    response_model=ModelInfoResponse,
    summary="Model metadata and feature list",
)
async def model_info() -> ModelInfoResponse:
    return ModelInfoResponse(
        version=MODEL_VERSION,
        type=ModelType.HEURISTIC,
        core_features=[
            "mean_heart_rate",
            "spo2_min",
            "mean_resp_rate",
            "mean_skin_temp_c",
            "motion_mean_g",
        ],
        extended_features=[
            "torso_elevation_mean_deg",
            "edge_proximity_max",
            "lower_body_movement_mean_g",
            "bed_pressure_shift_mean",
            "occupancy_confidence_mean",
        ],
        disclaimer=(
            "This service produces a heuristic DEMONSTRATION score from synthetic "
            "ambient sensor data using hand-tuned rules.  It is NOT a calibrated "
            "clinical probability and has NOT been clinically validated.  "
            "It must not be used for real clinical decision-making."
        ),
    )
