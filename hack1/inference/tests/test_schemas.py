"""
Tests for Pydantic schemas (SensorWindow validation).

Verifies:
- Valid windows parse correctly.
- Invalid duration is rejected.
- Array length mismatches are rejected.
- Out-of-bounds sample values are rejected.
- LOST/DEGRADED signal quality fields parse.
- Missing optional streams parse without error.
"""

from __future__ import annotations

import pytest
from pydantic import ValidationError

from app.models.schemas import SensorWindow, WINDOW_DURATION_MS
from tests.fixtures import (
    N_SAMPLES,
    WINDOW_END_MS,
    WINDOW_START_MS,
    build_window,
    flat,
    healthy_baseline_window,
    lost_signal_window,
    degraded_signal_window,
    core_only_window,
)


# ─────────────────────────────────────────────────────────────────────────────
#  Happy-path parsing
# ─────────────────────────────────────────────────────────────────────────────


def test_healthy_baseline_parses():
    """A fully-specified healthy window must parse without error."""
    raw = healthy_baseline_window()
    w = SensorWindow.model_validate(raw)
    assert w.window_id is not None
    assert w.sensors.heart_rate_bpm is not None
    assert len(w.sensors.heart_rate_bpm) == N_SAMPLES


def test_core_only_window_parses():
    """Window without optional streams must parse without error."""
    raw = core_only_window()
    w = SensorWindow.model_validate(raw)
    assert w.sensors.torso_elevation is None
    assert w.sensors.edge_proximity is None
    assert w.sensors.lower_body_movement is None
    assert w.sensors.bed_pressure_shift is None
    assert w.sensors.occupancy_confidence is None


def test_lost_signal_parses():
    raw = lost_signal_window()
    w = SensorWindow.model_validate(raw)
    assert w.metadata.signal_quality.value == "LOST"


def test_degraded_signal_parses():
    raw = degraded_signal_window()
    w = SensorWindow.model_validate(raw)
    assert w.metadata.signal_quality.value == "DEGRADED"


# ─────────────────────────────────────────────────────────────────────────────
#  Window duration validation
# ─────────────────────────────────────────────────────────────────────────────


def test_wrong_duration_rejected():
    """A window that is not exactly 300 000 ms must be rejected."""
    raw = build_window()
    raw["end_epoch_ms"] = raw["start_epoch_ms"] + 60_000  # 1 minute, not 5
    # Fix sensor lengths to match 1-minute window; use physiologically valid values
    physiological_defaults = {
        "heart_rate_bpm": 72.0,
        "spo2_pct": 98.0,
        "resp_rate_breaths": 15.0,
        "skin_temp_c": 33.5,   # must be ≥ 20 °C
        "motion_g": 0.05,
        "ambient_noise_db": 40.0,
        "light_lux": 200.0,
    }
    for key in list(raw["sensors"].keys()):
        default = physiological_defaults.get(key, 0.5)
        raw["sensors"][key] = flat(default, 60)
    with pytest.raises(ValidationError, match="duration must be"):
        SensorWindow.model_validate(raw)


def test_end_before_start_rejected():
    raw = build_window()
    raw["end_epoch_ms"] = raw["start_epoch_ms"] - 1
    raw["sensors"]["heart_rate_bpm"] = flat(72.0, 300)
    with pytest.raises(ValidationError, match="strictly greater"):
        SensorWindow.model_validate(raw)


# ─────────────────────────────────────────────────────────────────────────────
#  Array length validation
# ─────────────────────────────────────────────────────────────────────────────


def test_short_core_array_rejected():
    """Core array with wrong length must be rejected."""
    raw = build_window()
    raw["sensors"]["spo2_pct"] = flat(98.0, 150)   # half length
    with pytest.raises(ValidationError, match="length mismatch"):
        SensorWindow.model_validate(raw)


def test_short_optional_array_rejected():
    """Optional array with wrong length must be rejected."""
    raw = build_window(
        torso_elevation=flat(30.0, 150),   # wrong length
        edge_proximity=flat(0.2),
        lower_body_movement=flat(0.1),
        bed_pressure_shift=flat(0.1),
        occupancy_confidence=flat(0.9),
    )
    with pytest.raises(ValidationError, match="length mismatch"):
        SensorWindow.model_validate(raw)


def test_multiple_arrays_wrong_length_reports_all():
    """All mismatched arrays should appear in the error message."""
    raw = build_window()
    raw["sensors"]["heart_rate_bpm"] = flat(72.0, 10)
    raw["sensors"]["resp_rate_breaths"] = flat(14.0, 20)
    with pytest.raises(ValidationError) as exc_info:
        SensorWindow.model_validate(raw)
    msg = str(exc_info.value)
    assert "heart_rate_bpm" in msg
    assert "resp_rate_breaths" in msg


# ─────────────────────────────────────────────────────────────────────────────
#  Out-of-bounds sample values
# ─────────────────────────────────────────────────────────────────────────────


def test_heart_rate_above_bound_rejected():
    raw = build_window(heart_rate_bpm=flat(350.0))   # > 300 bpm limit
    with pytest.raises(ValidationError):
        SensorWindow.model_validate(raw)


def test_spo2_above_100_rejected():
    raw = build_window(spo2_pct=flat(101.0))
    with pytest.raises(ValidationError):
        SensorWindow.model_validate(raw)


def test_skin_temp_below_bound_rejected():
    raw = build_window(skin_temp_c=flat(10.0))   # < 20.0 °C limit
    with pytest.raises(ValidationError):
        SensorWindow.model_validate(raw)


def test_edge_proximity_above_1_rejected():
    raw = build_window(
        torso_elevation=flat(30.0),
        edge_proximity=flat(1.5),    # > 1.0 limit
        lower_body_movement=flat(0.1),
        bed_pressure_shift=flat(0.1),
        occupancy_confidence=flat(0.9),
    )
    with pytest.raises(ValidationError):
        SensorWindow.model_validate(raw)


# ─────────────────────────────────────────────────────────────────────────────
#  Sampling rate variation
# ─────────────────────────────────────────────────────────────────────────────


def test_half_hz_sampling_correct_length():
    """0.5 Hz × 300 s = 150 samples – must parse."""
    physiological_defaults = {
        "heart_rate_bpm": 72.0,
        "spo2_pct": 98.0,
        "resp_rate_breaths": 15.0,
        "skin_temp_c": 33.5,
        "motion_g": 0.05,
        "ambient_noise_db": 40.0,
        "light_lux": 200.0,
    }
    raw = build_window()
    raw["sampling_hz"] = 0.5
    n = 150
    for key in list(raw["sensors"].keys()):
        default = physiological_defaults.get(key, 0.5)
        raw["sensors"][key] = flat(default, n)
    w = SensorWindow.model_validate(raw)
    assert len(w.sensors.heart_rate_bpm) == n


def test_half_hz_sampling_wrong_length_rejected():
    """0.5 Hz but still 300 samples – must be rejected."""
    raw = build_window()
    raw["sampling_hz"] = 0.5
    # sensors still have 300 samples (default)
    with pytest.raises(ValidationError, match="length mismatch"):
        SensorWindow.model_validate(raw)
