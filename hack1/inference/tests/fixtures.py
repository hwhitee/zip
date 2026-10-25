"""
Shared test fixtures for the WardSentinel inference service.

All fixtures produce deterministic, synthetic sensor arrays.
No real patient data is used anywhere.

Window duration: 300 s at 1.0 Hz → 300 samples per array.
"""

from __future__ import annotations

import math
import uuid
from typing import Any

# ─────────────────────────────────────────────────────────────────────────────
#  Window timing constants
# ─────────────────────────────────────────────────────────────────────────────

WINDOW_START_MS: int = 1_700_000_000_000   # fixed epoch for reproducibility
WINDOW_END_MS: int = WINDOW_START_MS + 300_000
N_SAMPLES: int = 300   # 1.0 Hz × 300 s


# ─────────────────────────────────────────────────────────────────────────────
#  Sensor array generators
# ─────────────────────────────────────────────────────────────────────────────

def flat(value: float, n: int = N_SAMPLES) -> list[float]:
    """Constant array of length n."""
    return [value] * n


def ramp(start: float, end: float, n: int = N_SAMPLES) -> list[float]:
    """Linear ramp from start to end."""
    if n == 1:
        return [start]
    step = (end - start) / (n - 1)
    return [round(start + i * step, 6) for i in range(n)]


def sinusoid(
    mean: float,
    amplitude: float,
    period_samples: int = 60,
    n: int = N_SAMPLES,
) -> list[float]:
    """Sinusoidal variation around a mean value."""
    return [
        round(mean + amplitude * math.sin(2 * math.pi * i / period_samples), 6)
        for i in range(n)
    ]


# ─────────────────────────────────────────────────────────────────────────────
#  Base window builder
# ─────────────────────────────────────────────────────────────────────────────

def _base_metadata(quality: str = "GOOD") -> dict:
    return {
        "device_id": "SIM-DEVICE-001",
        "firmware_ver": "1.0.0",
        "signal_quality": quality,
    }


def build_window(
    *,
    patient_id: str | None = None,
    ward_id: str = "WARD-4B",
    bed_label: str = "04",
    quality: str = "GOOD",
    # core streams
    heart_rate_bpm: list[float] | None = None,
    spo2_pct: list[float] | None = None,
    resp_rate_breaths: list[float] | None = None,
    skin_temp_c: list[float] | None = None,
    motion_g: list[float] | None = None,
    ambient_noise_db: list[float] | None = None,
    light_lux: list[float] | None = None,
    # extended streams
    torso_elevation: list[float] | None = None,
    edge_proximity: list[float] | None = None,
    lower_body_movement: list[float] | None = None,
    bed_pressure_shift: list[float] | None = None,
    occupancy_confidence: list[float] | None = None,
) -> dict[str, Any]:
    """
    Build a raw SensorWindow dict suitable for JSON submission.
    Defaults produce a stable, healthy baseline for a resting adult.
    """
    sensors: dict[str, Any] = {
        "heart_rate_bpm": heart_rate_bpm or flat(72.0),
        "spo2_pct": spo2_pct or flat(98.0),
        "resp_rate_breaths": resp_rate_breaths or flat(15.0),
        "skin_temp_c": skin_temp_c or flat(33.5),
        "motion_g": motion_g or flat(0.05),
        "ambient_noise_db": ambient_noise_db or flat(40.0),
        "light_lux": light_lux or flat(200.0),
    }
    # Only include optional streams if explicitly provided
    if torso_elevation is not None:
        sensors["torso_elevation"] = torso_elevation
    if edge_proximity is not None:
        sensors["edge_proximity"] = edge_proximity
    if lower_body_movement is not None:
        sensors["lower_body_movement"] = lower_body_movement
    if bed_pressure_shift is not None:
        sensors["bed_pressure_shift"] = bed_pressure_shift
    if occupancy_confidence is not None:
        sensors["occupancy_confidence"] = occupancy_confidence

    return {
        "window_id": str(uuid.uuid4()),
        "patient_id": patient_id or str(uuid.uuid4()),
        "ward_id": ward_id,
        "bed_label": bed_label,
        "start_epoch_ms": WINDOW_START_MS,
        "end_epoch_ms": WINDOW_END_MS,
        "sampling_hz": 1.0,
        "sensors": sensors,
        "metadata": _base_metadata(quality),
    }


# ─────────────────────────────────────────────────────────────────────────────
#  Named scenario fixtures
# ─────────────────────────────────────────────────────────────────────────────

def healthy_baseline_window() -> dict[str, Any]:
    """Normal resting adult – expect LOW risk."""
    return build_window(
        heart_rate_bpm=flat(70.0),
        spo2_pct=flat(98.5),
        resp_rate_breaths=flat(14.0),
        skin_temp_c=flat(33.5),
        motion_g=flat(0.04),
        torso_elevation=flat(30.0),
        edge_proximity=flat(0.15),
        lower_body_movement=flat(0.03),
        bed_pressure_shift=flat(0.1),
        occupancy_confidence=flat(0.98),
    )


def safe_repositioning_window() -> dict[str, Any]:
    """
    SAFE_REPOSITIONING scenario:
    - Torso elevation rising from 20° to 45° (nurse-assisted turn)
    - Moderate bed pressure shift (redistribution underway)
    - Edge proximity low (patient not near edge)
    - Lower body movement moderate (limbs being moved)
    """
    return build_window(
        heart_rate_bpm=flat(78.0),
        spo2_pct=flat(97.0),
        resp_rate_breaths=flat(16.0),
        skin_temp_c=flat(34.0),
        motion_g=sinusoid(0.10, 0.05, period_samples=30),
        torso_elevation=ramp(20.0, 45.0),          # rising, in [15, 55]
        edge_proximity=flat(0.20),                  # < 0.35 ✓
        lower_body_movement=flat(0.25),             # < 0.5 ✓
        bed_pressure_shift=flat(0.35),              # > 0.25 ✓
        occupancy_confidence=flat(0.95),
    )


def progressive_bed_exit_window() -> dict[str, Any]:
    """
    PROGRESSIVE_BED_EXIT scenario:
    - Edge proximity rising toward 0.75 (patient shifting to edge)
    - Torso elevation rising sharply toward 65°
    - Lower body movement increasing (legs dangling)
    - Occupancy confidence declining (sensor losing coverage)
    """
    return build_window(
        heart_rate_bpm=flat(88.0),
        spo2_pct=flat(96.0),
        resp_rate_breaths=flat(18.0),
        skin_temp_c=flat(34.2),
        motion_g=ramp(0.05, 0.35),
        torso_elevation=ramp(20.0, 65.0),           # > 40 at end ✓
        edge_proximity=ramp(0.30, 0.75),            # > 0.6 at end ✓
        lower_body_movement=ramp(0.10, 0.55),       # > 0.4 at end ✓
        bed_pressure_shift=ramp(0.05, 0.50),
        occupancy_confidence=ramp(0.98, 0.55),      # < 0.65 at end ✓
    )


def critical_vitals_window() -> dict[str, Any]:
    """Severely abnormal vitals – expect HIGH or CRITICAL risk."""
    return build_window(
        heart_rate_bpm=flat(130.0),   # tachycardia
        spo2_pct=flat(88.0),          # severe desaturation
        resp_rate_breaths=flat(28.0), # tachypnoea
        skin_temp_c=flat(38.5),       # elevated
        motion_g=flat(0.01),          # immobile
        torso_elevation=flat(5.0),    # flat
        edge_proximity=flat(0.10),
        lower_body_movement=flat(0.01),
        bed_pressure_shift=flat(0.02),
        occupancy_confidence=flat(0.99),
    )


def degraded_signal_window() -> dict[str, Any]:
    """DEGRADED signal quality – valid vitals but elevated uncertainty."""
    return build_window(
        quality="DEGRADED",
        heart_rate_bpm=flat(75.0),
        spo2_pct=flat(97.5),
        resp_rate_breaths=flat(15.0),
        skin_temp_c=flat(33.5),
        motion_g=flat(0.05),
    )


def lost_signal_window() -> dict[str, Any]:
    """LOST signal – inference must skip scoring."""
    return build_window(
        quality="LOST",
        heart_rate_bpm=flat(72.0),
        spo2_pct=flat(98.0),
        resp_rate_breaths=flat(14.0),
        skin_temp_c=flat(33.5),
        motion_g=flat(0.05),
    )


def core_only_window() -> dict[str, Any]:
    """Only core 7 streams present – no extended modalities."""
    return build_window(
        heart_rate_bpm=flat(70.0),
        spo2_pct=flat(98.0),
        resp_rate_breaths=flat(14.0),
        skin_temp_c=flat(33.5),
        motion_g=flat(0.05),
        # No extended streams → all five will appear in missing_modalities
    )
