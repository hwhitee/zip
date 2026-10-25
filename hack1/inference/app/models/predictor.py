"""
WardSentinel – Heuristic Predictor

DISCLAIMER
----------
This predictor produces a DEMONSTRATION risk score derived from synthetic
ambient sensor data using hand-tuned heuristic rules and normalised
z-score components.  It is NOT a calibrated clinical probability, has NOT
been trained on patient data, and has NOT undergone clinical validation.
It must not be used for real clinical decision-making.

Architecture
------------
The predictor is deliberately deterministic so that:
  - Unit tests can assert exact outputs.
  - Demo scenarios reproduce identically on every run.
  - The Spring Boot layer can compare successive windows without drift.

Scoring pipeline
----------------
1.  Feature extraction   – compute per-stream statistics (mean, std, min, max,
                           trend slope) using NumPy.  Missing optional streams
                           are flagged, not zero-filled.

2.  Component scoring    – each feature produces a component score in [0, 1]
                           using a hand-tuned sigmoid or linear clamp.

3.  Weighted sum         – components are combined with fixed weights that sum
                           to 1.0.  Missing modalities reduce the total weight
                           and increase the uncertainty estimate.

4.  Scenario detection   – the window is compared against two deterministic
                           demo fixtures (SAFE_REPOSITIONING, PROGRESSIVE_BED_EXIT).
                           If matched, the scenario_hint field is populated.
                           Scenario detection DOES NOT alter the score.

5.  Risk classification  – raw score is clamped to [0, 1] and classified
                           against the thresholds from the contract.
"""

from __future__ import annotations

import math
from dataclasses import dataclass, field
from typing import Final

import numpy as np

from app.models.schemas import (
    ContributingFactor,
    ModelType,
    RiskLevel,
    RiskThresholds,
    SensorWindow,
    SignalQuality,
)

# ─────────────────────────────────────────────────────────────────────────────
#  Constants
# ─────────────────────────────────────────────────────────────────────────────

MODEL_VERSION: Final[str] = "v0.1.0-heuristic"
THRESHOLDS: Final[RiskThresholds] = RiskThresholds()

# Component weights – must sum to 1.0
# Core (required) streams account for 0.70, extended for 0.30.
_CORE_WEIGHT: Final[float] = 0.70
_EXT_WEIGHT: Final[float] = 0.30

# Individual component weights (core)
_W: Final[dict[str, float]] = {
    # core
    "heart_rate":    0.18,
    "spo2":          0.18,
    "resp_rate":     0.14,
    "skin_temp":     0.10,
    "motion":        0.10,
    # extended (fall-risk / pressure-injury)
    "torso_elevation":      0.08,
    "edge_proximity":       0.10,
    "lower_body_movement":  0.06,
    "bed_pressure_shift":   0.04,
    "occupancy_confidence": 0.02,
}
assert abs(sum(_W.values()) - 1.0) < 1e-9, "Weights must sum to 1.0"

# Physiological reference ranges used for z-score normalisation
# (mid-range resting adult; not personalised)
_REF: Final[dict[str, tuple[float, float]]] = {
    # (mean, std) of the "normal" population distribution used to compute z
    "heart_rate_bpm":  (72.0, 12.0),
    "spo2_pct":        (97.5, 1.5),   # inverted – lower is worse
    "resp_rate":       (16.0, 3.0),
    "skin_temp_c":     (33.5, 1.0),
    "motion_g":        (0.05, 0.03),  # at-rest motion level
}

# Scenario matching tolerance
_SCENARIO_TOL: Final[float] = 0.05   # absolute tolerance on normalised feature values


# ─────────────────────────────────────────────────────────────────────────────
#  Internal result dataclass
# ─────────────────────────────────────────────────────────────────────────────

@dataclass
class _ScoringResult:
    risk_score: float
    uncertainty: float
    factors: list[ContributingFactor]
    missing_modalities: list[str]
    warnings: list[str]
    scenario_hint: str | None
    model_type: ModelType


# ─────────────────────────────────────────────────────────────────────────────
#  Helper maths
# ─────────────────────────────────────────────────────────────────────────────

def _sigmoid(x: float, k: float = 3.0) -> float:
    """Logistic sigmoid scaled by k; maps R → (0, 1)."""
    return 1.0 / (1.0 + math.exp(-k * x))


def _clamp(v: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


def _z_score_component(
    arr: np.ndarray,
    ref_mean: float,
    ref_std: float,
    *,
    invert: bool = False,
) -> tuple[float, float]:
    """
    Compute a [0,1] risk component from a sensor array.

    Steps:
      1. Compute the mean of the array.
      2. Compute normalised deviation: z = (mean - ref_mean) / ref_std
      3. Pass through sigmoid to get [0, 1].

    Returns (component_score, raw_value_used).
    invert=True means a lower raw value → higher risk (e.g. SpO2).
    """
    raw = float(np.mean(arr))
    z = (raw - ref_mean) / ref_std
    if invert:
        z = -z
    return _sigmoid(z), raw


def _trend_slope(arr: np.ndarray) -> float:
    """
    Linear regression slope over the array (normalised to array length).
    Returns slope in units-per-sample.
    """
    n = len(arr)
    if n < 2:
        return 0.0
    x = np.arange(n, dtype=float)
    # np.polyfit degree 1
    slope, _ = np.polyfit(x, arr, 1)
    return float(slope)


# ─────────────────────────────────────────────────────────────────────────────
#  Core stream scoring
# ─────────────────────────────────────────────────────────────────────────────

def _score_heart_rate(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    score, raw = _z_score_component(
        arr, *_REF["heart_rate_bpm"], invert=False
    )
    # Tachycardia (>100) and bradycardia (<50) both raise risk
    mean_hr = float(np.mean(arr))
    if mean_hr > 100 or mean_hr < 50:
        score = _clamp(score + 0.15)
    return score, ContributingFactor(
        feature_name="mean_heart_rate",
        value=round(mean_hr, 2),
        weight=_W["heart_rate"],
    )


def _score_spo2(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    min_spo2 = float(np.min(arr))
    # Desaturation risk: SpO2 < 94 is clinically concerning
    desaturation_z = (97.5 - min_spo2) / 1.5          # positive = worse
    score = _clamp(_sigmoid(desaturation_z))
    return score, ContributingFactor(
        feature_name="spo2_min",
        value=round(min_spo2, 2),
        weight=_W["spo2"],
    )


def _score_resp_rate(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    score, raw = _z_score_component(arr, *_REF["resp_rate"], invert=False)
    mean_rr = float(np.mean(arr))
    # Tachypnoea (>20) more dangerous than bradypnoea in ward context
    if mean_rr > 20:
        score = _clamp(score + 0.10)
    return score, ContributingFactor(
        feature_name="mean_resp_rate",
        value=round(mean_rr, 2),
        weight=_W["resp_rate"],
    )


def _score_skin_temp(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    score, raw = _z_score_component(arr, *_REF["skin_temp_c"], invert=False)
    return score, ContributingFactor(
        feature_name="mean_skin_temp_c",
        value=round(raw, 2),
        weight=_W["skin_temp"],
    )


def _score_motion(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    """
    For ward safety, motion is a nuanced signal:
    - Very low motion (immobility) → pressure-injury risk (raises score)
    - Very high motion (agitation) → fall risk (raises score)
    - Moderate motion (expected turning) → lower score
    """
    mean_g = float(np.mean(arr))
    std_g = float(np.std(arr))

    # Immobility penalty: mean < 0.02 g sustained
    immobility = _clamp(1.0 - (mean_g / 0.05))

    # Agitation penalty: mean > 0.3 g
    agitation_z = (mean_g - 0.15) / 0.08
    agitation = _clamp(_sigmoid(agitation_z))

    # Take the maximum risk signal
    score = _clamp(max(immobility * 0.6, agitation * 0.8))
    return score, ContributingFactor(
        feature_name="motion_mean_g",
        value=round(mean_g, 4),
        weight=_W["motion"],
    )


# ─────────────────────────────────────────────────────────────────────────────
#  Extended stream scoring
# ─────────────────────────────────────────────────────────────────────────────

def _score_torso_elevation(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    """
    Torso elevation context:
    - 0–10°: flat – sustained may indicate immobility
    - 30–45°: normal repositioned / semi-fowler
    - >60°: upright – could precede bed exit
    Score peaks at extremes (very flat sustained, or very upright with movement).
    """
    mean_deg = float(np.mean(arr))
    slope = _trend_slope(arr)

    # Flat + sustained = pressure risk
    flat_risk = _clamp(1.0 - mean_deg / 45.0) if mean_deg < 45 else 0.0

    # Rising steeply = bed exit precursor
    rising_risk = _clamp(slope * 20.0) if slope > 0.01 else 0.0

    score = _clamp(max(flat_risk * 0.5, rising_risk))
    return score, ContributingFactor(
        feature_name="torso_elevation_mean_deg",
        value=round(mean_deg, 2),
        weight=_W["torso_elevation"],
    )


def _score_edge_proximity(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    """
    Proximity to bed edge: score rises sharply when patient is close to the edge
    and trending towards it.
    """
    max_prox = float(np.max(arr))
    slope = _trend_slope(arr)
    trend_penalty = _clamp(slope * 5.0) if slope > 0 else 0.0
    # Edge proximity > 0.75 is high risk regardless of trend
    proximity_score = _clamp(_sigmoid((max_prox - 0.7) / 0.1))
    score = _clamp(proximity_score + trend_penalty * 0.3)
    return score, ContributingFactor(
        feature_name="edge_proximity_max",
        value=round(max_prox, 3),
        weight=_W["edge_proximity"],
    )


def _score_lower_body_movement(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    """
    Lower-body motion: high variance + increasing trend suggests active exit attempt.
    """
    mean_g = float(np.mean(arr))
    std_g = float(np.std(arr))
    slope = _trend_slope(arr)

    activity_score = _clamp(_sigmoid((mean_g - 0.2) / 0.1))
    variability_score = _clamp(std_g / 0.5)
    trend_score = _clamp(slope * 10.0) if slope > 0 else 0.0

    score = _clamp(activity_score * 0.5 + variability_score * 0.3 + trend_score * 0.2)
    return score, ContributingFactor(
        feature_name="lower_body_movement_mean_g",
        value=round(mean_g, 4),
        weight=_W["lower_body_movement"],
    )


def _score_bed_pressure_shift(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    """
    Pressure redistribution: rapid or large shifts may indicate repositioning
    attempt or early fall.  Very low shifts (near zero) = sustained pressure = risk.
    """
    mean_shift = float(np.mean(arr))
    max_shift = float(np.max(arr))
    slope = _trend_slope(arr)

    # Sustained no-shift = immobility risk
    immobility_risk = _clamp(1.0 - mean_shift * 3.0) if mean_shift < 0.33 else 0.0

    # Rapid large shift = instability risk
    instability_risk = _clamp(_sigmoid((max_shift - 0.6) / 0.1))

    score = _clamp(max(immobility_risk * 0.6, instability_risk * 0.9))
    return score, ContributingFactor(
        feature_name="bed_pressure_shift_mean",
        value=round(mean_shift, 3),
        weight=_W["bed_pressure_shift"],
    )


def _score_occupancy_confidence(arr: np.ndarray) -> tuple[float, ContributingFactor]:
    """
    Occupancy confidence: a declining trend suggests the patient may be exiting
    the bed (or has already fallen out of sensor range).
    Sustained low confidence with motion is very high risk.
    """
    mean_conf = float(np.mean(arr))
    slope = _trend_slope(arr)

    # Declining occupancy is the key signal
    decline_score = _clamp(-slope * 30.0) if slope < 0 else 0.0

    # Very low confidence = possibly already out of bed
    low_conf_score = _clamp(1.0 - mean_conf)

    score = _clamp(decline_score * 0.7 + low_conf_score * 0.3)
    return score, ContributingFactor(
        feature_name="occupancy_confidence_mean",
        value=round(mean_conf, 3),
        weight=_W["occupancy_confidence"],
    )


# ─────────────────────────────────────────────────────────────────────────────
#  Scenario detection
# ─────────────────────────────────────────────────────────────────────────────

def _detect_scenario(
    factors: list[ContributingFactor],
    missing: list[str],
) -> str | None:
    """
    Check if the extracted features match a known demo scenario pattern.
    Returns a scenario hint string, or None.

    SAFE_REPOSITIONING:
      - torso_elevation rising, moderate (20–50°)
      - bed_pressure_shift high (redistribution)
      - edge_proximity low (< 0.3)
      - lower_body_movement moderate

    PROGRESSIVE_BED_EXIT:
      - edge_proximity high (max > 0.6) and rising
      - lower_body_movement mean > 0.28 (actively moving legs)
      - occupancy_confidence mean < 0.80 (sensor losing coverage)
      - torso_elevation mean > 35° and rising
    """
    if missing:
        # Cannot detect scenario if any extended modality is absent
        return None

    fmap = {f.feature_name: f.value for f in factors}

    torso = fmap.get("torso_elevation_mean_deg", 0.0)
    edge = fmap.get("edge_proximity_max", 0.0)
    lbm = fmap.get("lower_body_movement_mean_g", 0.0)
    pressure = fmap.get("bed_pressure_shift_mean", 0.0)
    occ = fmap.get("occupancy_confidence_mean", 1.0)

    # SAFE_REPOSITIONING: torso in [15, 55], edge < 0.35, pressure shift > 0.25
    if 15 <= torso <= 55 and edge < 0.35 and pressure > 0.25 and lbm < 0.5:
        return "SAFE_REPOSITIONING"

    # PROGRESSIVE_BED_EXIT:
    #   edge_proximity_max > 0.6   (patient reaching bed edge)
    #   lower_body_movement_mean > 0.28  (legs actively moving)
    #   occupancy_confidence_mean < 0.80 (sensor losing coverage – declining)
    #   torso_elevation_mean > 35         (torso rising toward seated/upright)
    if edge > 0.6 and lbm > 0.28 and occ < 0.80 and torso > 35:
        return "PROGRESSIVE_BED_EXIT"

    return None


# ─────────────────────────────────────────────────────────────────────────────
#  Public scoring function
# ─────────────────────────────────────────────────────────────────────────────

def score_window(window: SensorWindow) -> _ScoringResult:
    """
    Entry point: score a SensorWindow and return a _ScoringResult.

    Signal quality handling
    -----------------------
    LOST  → returns fixed score 0.50, model_type=SKIPPED, high uncertainty.
            Never substitutes real values.
    DEGRADED → scores normally but adds warning and raises uncertainty by 0.2.
    GOOD  → full scoring.

    Missing optional modalities
    ---------------------------
    If any extended stream is None, it is excluded from the weighted sum
    and listed in missing_modalities.  The effective weight denominator is
    reduced accordingly (not zero-padded).  Uncertainty is increased.
    """
    warnings: list[str] = []
    missing_modalities: list[str] = []
    s = window.sensors
    quality = window.metadata.signal_quality

    # ── LOST signal: skip scoring entirely ────────────────────────────────────
    if quality == SignalQuality.LOST:
        return _ScoringResult(
            risk_score=0.50,
            uncertainty=1.0,
            factors=[],
            missing_modalities=[],
            warnings=[
                "Signal quality is LOST. Scoring skipped. "
                "Score fixed at 0.50 (maximum uncertainty). "
                "Physical check recommended."
            ],
            scenario_hint=None,
            model_type=ModelType.SKIPPED,
        )

    if quality == SignalQuality.DEGRADED:
        warnings.append(
            "Signal quality is DEGRADED. Score reliability is reduced. "
            "Uncertainty is elevated."
        )

    # ── Core stream scoring ───────────────────────────────────────────────────
    hr_score, hr_factor = _score_heart_rate(np.array(s.heart_rate_bpm))
    sp_score, sp_factor = _score_spo2(np.array(s.spo2_pct))
    rr_score, rr_factor = _score_resp_rate(np.array(s.resp_rate_breaths))
    st_score, st_factor = _score_skin_temp(np.array(s.skin_temp_c))
    mo_score, mo_factor = _score_motion(np.array(s.motion_g))

    factors: list[ContributingFactor] = [hr_factor, sp_factor, rr_factor, st_factor, mo_factor]

    weighted_sum = (
        hr_score * _W["heart_rate"]
        + sp_score * _W["spo2"]
        + rr_score * _W["resp_rate"]
        + st_score * _W["skin_temp"]
        + mo_score * _W["motion"]
    )
    active_weight = _CORE_WEIGHT  # 0.70 from core streams

    # ── Extended stream scoring (degrade gracefully if absent) ────────────────
    extended_checks: list[
        tuple[str, list[float] | None, str, callable]
    ] = [
        ("torso_elevation", s.torso_elevation, "torso_elevation", _score_torso_elevation),
        ("edge_proximity", s.edge_proximity, "edge_proximity", _score_edge_proximity),
        ("lower_body_movement", s.lower_body_movement, "lower_body_movement", _score_lower_body_movement),
        ("bed_pressure_shift", s.bed_pressure_shift, "bed_pressure_shift", _score_bed_pressure_shift),
        ("occupancy_confidence", s.occupancy_confidence, "occupancy_confidence", _score_occupancy_confidence),
    ]

    for stream_name, stream_data, weight_key, scorer in extended_checks:
        if stream_data is None:
            missing_modalities.append(stream_name)
            # NOTE: weight is simply omitted – NOT replaced with a safe zero
        else:
            ext_score, ext_factor = scorer(np.array(stream_data))
            factors.append(ext_factor)
            weighted_sum += ext_score * _W[weight_key]
            active_weight += _W[weight_key]

    if missing_modalities:
        warnings.append(
            f"Missing optional sensor streams: {', '.join(missing_modalities)}. "
            "Extended ward-safety features excluded from score. "
            "Score reflects core vitals only. Uncertainty elevated."
        )

    # ── Normalise to [0, 1] over the weight actually used ────────────────────
    if active_weight > 0:
        raw_score = _clamp(weighted_sum / active_weight)
    else:
        raw_score = 0.5   # degenerate: no data at all

    # Round to 5 decimal places (contract specification)
    risk_score = round(raw_score, 5)

    # ── Uncertainty estimate ──────────────────────────────────────────────────
    missing_fraction = len(missing_modalities) / len(extended_checks)
    base_uncertainty = missing_fraction * 0.4
    if quality == SignalQuality.DEGRADED:
        base_uncertainty = min(1.0, base_uncertainty + 0.20)
    uncertainty = round(_clamp(base_uncertainty), 4)

    # ── Scenario detection ────────────────────────────────────────────────────
    scenario_hint = _detect_scenario(factors, missing_modalities)

    return _ScoringResult(
        risk_score=risk_score,
        uncertainty=uncertainty,
        factors=factors,
        missing_modalities=missing_modalities,
        warnings=warnings,
        scenario_hint=scenario_hint,
        model_type=ModelType.HEURISTIC,
    )


# ─────────────────────────────────────────────────────────────────────────────
#  Risk classification
# ─────────────────────────────────────────────────────────────────────────────

def classify_risk(score: float, thresholds: RiskThresholds = THRESHOLDS) -> RiskLevel:
    """Map a [0,1] score to a RiskLevel using the contract thresholds."""
    if score >= thresholds.high:        # 0.80
        return RiskLevel.CRITICAL
    if score >= thresholds.elevated:    # 0.60
        return RiskLevel.HIGH
    if score >= thresholds.low:         # 0.35
        return RiskLevel.ELEVATED
    return RiskLevel.LOW
