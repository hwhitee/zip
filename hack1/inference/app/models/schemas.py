"""
WardSentinel Inference – Pydantic schemas.

These types are the canonical Python representation of the shared contract
defined in docs/api-contracts.md.  Do NOT alter field names here without
updating the contract document and the TypeScript mirror in
frontend/src/lib/types.ts.

IMPORTANT: This service produces a heuristic DEMONSTRATION score.
           It is NOT a calibrated clinical probability and has NOT
           undergone clinical validation.
"""

from __future__ import annotations

from enum import Enum
from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator
from pydantic.alias_generators import to_camel


# ─────────────────────────────────────────────────────────────────────────────
#  Enumerations
# ─────────────────────────────────────────────────────────────────────────────


class SignalQuality(str, Enum):
    GOOD = "GOOD"
    DEGRADED = "DEGRADED"
    LOST = "LOST"


class RiskLevel(str, Enum):
    LOW = "LOW"
    ELEVATED = "ELEVATED"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class ModelType(str, Enum):
    HEURISTIC = "HEURISTIC"
    ML = "ML"
    SKIPPED = "SKIPPED"


# ─────────────────────────────────────────────────────────────────────────────
#  Input – SensorWindow
# ─────────────────────────────────────────────────────────────────────────────

# Individual sensor reading bounds (physiologically plausible, not clinical limits)
HeartRateSample = Annotated[float, Field(ge=0.0, le=300.0)]
SpO2Sample = Annotated[float, Field(ge=0.0, le=100.0)]
RespRateSample = Annotated[float, Field(ge=0.0, le=120.0)]
SkinTempSample = Annotated[float, Field(ge=20.0, le=45.0)]
MotionSample = Annotated[float, Field(ge=0.0, le=20.0)]   # g
NoiseSample = Annotated[float, Field(ge=0.0, le=140.0)]   # dB SPL
LightSample = Annotated[float, Field(ge=0.0, le=200_000.0)]  # lux

# Clinical fall-risk / pressure-injury ambient sensor extensions
# (the new sensors requested for the WardSentinel demo scenarios)
TorsoElevationSample = Annotated[float, Field(ge=0.0, le=90.0)]   # degrees from horizontal
EdgeProximitySample = Annotated[float, Field(ge=0.0, le=1.0)]     # 0 = centre, 1 = bed edge
LowerBodyMovementSample = Annotated[float, Field(ge=0.0, le=10.0)]  # g (lower-body accel)
BedPressureShiftSample = Annotated[float, Field(ge=0.0, le=1.0)]   # 0 = static, 1 = full shift
OccupancyConfidenceSample = Annotated[float, Field(ge=0.0, le=1.0)]  # 0 = empty, 1 = occupied


class SensorPayload(BaseModel):
    """
    Seven core ambient sensor streams from the contract, plus five new
    ward-safety streams required for SAFE_REPOSITIONING / BED_EXIT scenarios.

    All arrays must have equal length (validated at the SensorWindow level).
    Missing optional streams are represented as None (not zero).
    The predictor handles missing modalities explicitly and emits a warning.
    """

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    # ── Core streams (required by contract) ───────────────────────────────────
    heart_rate_bpm: list[HeartRateSample] = Field(
        min_length=1,
        description="Heart rate samples in beats-per-minute",
    )
    spo2_pct: list[SpO2Sample] = Field(
        min_length=1,
        description="Peripheral O2 saturation percentage",
    )
    resp_rate_breaths: list[RespRateSample] = Field(
        min_length=1,
        description="Respiratory rate in breaths per minute",
    )
    skin_temp_c: list[SkinTempSample] = Field(
        min_length=1,
        description="Skin temperature in degrees Celsius",
    )
    motion_g: list[MotionSample] = Field(
        min_length=1,
        description="Whole-body accelerometer magnitude in g",
    )
    ambient_noise_db: list[NoiseSample] = Field(
        min_length=1,
        description="Ambient sound level in dB SPL",
    )
    light_lux: list[LightSample] = Field(
        min_length=1,
        description="Ambient illuminance in lux",
    )

    # ── Extended ward-safety streams (optional, degraded-mode if absent) ──────
    torso_elevation: list[TorsoElevationSample] | None = Field(
        default=None,
        description="Torso elevation angle in degrees from horizontal (0=flat, 90=seated).",
    )
    edge_proximity: list[EdgeProximitySample] | None = Field(
        default=None,
        description="Normalised distance from bed edge (0=centre, 1=at edge).",
    )
    lower_body_movement: list[LowerBodyMovementSample] | None = Field(
        default=None,
        description="Lower-body accelerometer magnitude in g.",
    )
    bed_pressure_shift: list[BedPressureShiftSample] | None = Field(
        default=None,
        description="Bed mat pressure redistribution score (0=static, 1=full shift).",
    )
    occupancy_confidence: list[OccupancyConfidenceSample] | None = Field(
        default=None,
        description="Bed occupancy confidence score (0=empty, 1=definitely occupied).",
    )


class SensorWindowMetadata(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    device_id: str = Field(max_length=64)
    firmware_ver: str = Field(max_length=32)
    signal_quality: SignalQuality = SignalQuality.GOOD


# Expected window duration in milliseconds (5 minutes)
WINDOW_DURATION_MS: int = 300_000
MIN_SAMPLING_HZ: float = 0.1
MAX_SAMPLING_HZ: float = 100.0


class SensorWindow(BaseModel):
    """
    A fixed-width time window of ambient sensor readings for one patient.
    Mirrors the TypeScript SensorWindow interface in docs/api-contracts.md.
    """

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    window_id: UUID = Field(description="Producer-assigned UUID v4 — idempotent on re-submission")
    patient_id: UUID
    ward_id: str = Field(min_length=1, max_length=32)
    bed_label: str = Field(min_length=1, max_length=8)
    start_epoch_ms: int = Field(ge=0)
    end_epoch_ms: int = Field(ge=0)
    sampling_hz: float = Field(default=1.0, ge=MIN_SAMPLING_HZ, le=MAX_SAMPLING_HZ)
    schema_version: str | None = Field(default="1.0.0")
    sequence_number: int | None = Field(default=0)
    idempotency_key: str | None = Field(default=None)
    sensors: SensorPayload
    metadata: SensorWindowMetadata = Field(default_factory=SensorWindowMetadata)

    @model_validator(mode="after")
    def _validate_window(self) -> "SensorWindow":
        # 1. Time ordering
        if self.end_epoch_ms <= self.start_epoch_ms:
            raise ValueError("end_epoch_ms must be strictly greater than start_epoch_ms")

        # 2. Duration must equal WINDOW_DURATION_MS
        duration = self.end_epoch_ms - self.start_epoch_ms
        if duration != WINDOW_DURATION_MS:
            raise ValueError(
                f"Window duration must be {WINDOW_DURATION_MS} ms "
                f"(5 minutes), got {duration} ms"
            )

        # 3. All required sensor arrays must have equal length
        expected_len = round(self.sampling_hz * (WINDOW_DURATION_MS / 1000))
        core_arrays = {
            "heart_rate_bpm": self.sensors.heart_rate_bpm,
            "spo2_pct": self.sensors.spo2_pct,
            "resp_rate_breaths": self.sensors.resp_rate_breaths,
            "skin_temp_c": self.sensors.skin_temp_c,
            "motion_g": self.sensors.motion_g,
            "ambient_noise_db": self.sensors.ambient_noise_db,
            "light_lux": self.sensors.light_lux,
        }
        bad = [
            f"{name}(len={len(arr)}, expected {expected_len})"
            for name, arr in core_arrays.items()
            if len(arr) != expected_len
        ]
        if bad:
            raise ValueError(
                f"Sensor array length mismatch for sampling_hz={self.sampling_hz}: "
                + ", ".join(bad)
            )

        # 4. Optional arrays, if present, must also match
        optional_arrays = {
            "torso_elevation": self.sensors.torso_elevation,
            "edge_proximity": self.sensors.edge_proximity,
            "lower_body_movement": self.sensors.lower_body_movement,
            "bed_pressure_shift": self.sensors.bed_pressure_shift,
            "occupancy_confidence": self.sensors.occupancy_confidence,
        }
        bad_opt = [
            f"{name}(len={len(arr)}, expected {expected_len})"
            for name, arr in optional_arrays.items()
            if arr is not None and len(arr) != expected_len
        ]
        if bad_opt:
            raise ValueError(
                f"Optional sensor array length mismatch: " + ", ".join(bad_opt)
            )

        return self


# ─────────────────────────────────────────────────────────────────────────────
#  Output – InferenceResult
# ─────────────────────────────────────────────────────────────────────────────


class ContributingFactor(BaseModel):
    """One feature's contribution to the risk score."""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    feature_name: str
    value: float
    weight: float = Field(
        description="Signed importance weight; positive = raises risk, negative = lowers risk"
    )


class RiskThresholds(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    low: float = 0.35
    elevated: float = 0.60
    high: float = 0.80


class StructuredExplanation(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    summary: str = Field(
        description="Concise 1-2 sentence non-diagnostic summary of sensor trends"
    )
    supporting_evidence: list[str] = Field(
        default_factory=list,
        description="Key derived ambient features contributing to the trend"
    )
    limitations: list[str] = Field(
        default_factory=list,
        description="Explicit disclaimers: heuristic demo, uncalibrated probability, not clinical diagnosis"
    )
    missing_data: list[str] = Field(
        default_factory=list,
        description="List of omitted or degraded streams"
    )
    recommended_human_review: list[str] = Field(
        default_factory=list,
        description="Recommended observational checks by human staff at bedside"
    )


class InferenceResult(BaseModel):
    """
    Output of the heuristic predictor.
    Mirrors the TypeScript InferenceResult interface in docs/api-contracts.md.

    DISCLAIMER: risk_score is a heuristic DEMONSTRATION value computed from
    synthetic sensor data.  It is NOT a calibrated clinical probability and
    has NOT been clinically validated.
    """

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    result_id: UUID
    window_id: UUID
    patient_id: UUID

    computed_at_ms: int
    latency_ms: int

    risk_score: Annotated[float, Field(ge=0.0, le=1.0)] | None = Field(
        default=None,
        description="Heuristic demonstration risk score in [0.0, 1.0]. None if model execution was skipped or degraded.",
    )
    risk_level: RiskLevel
    risk_thresholds: RiskThresholds = Field(default_factory=RiskThresholds)

    contributing_factors: list[ContributingFactor]

    # ── Extended fields (beyond contract minimum) ─────────────────────────────
    uncertainty: Annotated[float, Field(ge=0.0, le=1.0)] = Field(
        description="Heuristic uncertainty estimate (0=confident, 1=maximally uncertain). "
                    "Higher when signal is DEGRADED or modalities are missing."
    )
    missing_modalities: list[str] = Field(
        default_factory=list,
        description="Names of optional sensor streams absent from this window.",
    )
    warnings: list[str] = Field(
        default_factory=list,
        description="Non-fatal issues detected during scoring (e.g. degraded signal).",
    )
    scenario_hint: str | None = Field(
        default=None,
        description="Demo scenario identifier if the pattern matches a known fixture "
                    "(e.g. SAFE_REPOSITIONING, PROGRESSIVE_BED_EXIT). None in normal operation.",
    )

    gemini_explanation: str | None = None

    model_version: str
    model_type: ModelType


# ─────────────────────────────────────────────────────────────────────────────
#  API utility schemas
# ─────────────────────────────────────────────────────────────────────────────


class HealthResponse(BaseModel):
    status: Literal["ok", "degraded"]
    model_version: str
    uptime_seconds: float


class ModelInfoResponse(BaseModel):
    version: str
    type: ModelType
    core_features: list[str]
    extended_features: list[str]
    disclaimer: str


class ApiError(BaseModel):
    """Uniform error envelope — matches the contract for all services."""
    timestamp: str
    status: int
    error: str
    message: str
    path: str
    trace_id: str | None = None
