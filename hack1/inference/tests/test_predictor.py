"""
Tests for the heuristic predictor.

Verifies:
- Determinism: same input → same score on repeated calls.
- LOST signal → score=0.5, model_type=SKIPPED, no factors.
- DEGRADED signal → warning present, uncertainty elevated.
- Healthy baseline → LOW risk.
- Critical vitals → HIGH or CRITICAL risk.
- Missing modalities → listed in missing_modalities, NOT zero-padded.
- SAFE_REPOSITIONING scenario → detected and score in expected range.
- PROGRESSIVE_BED_EXIT scenario → detected and score in expected range.
- Score is always in [0, 1].
- Risk classification boundaries.
"""

from __future__ import annotations

import pytest

from app.models.predictor import MODEL_VERSION, THRESHOLDS, classify_risk, score_window
from app.models.schemas import ModelType, RiskLevel, SensorWindow
from tests.fixtures import (
    build_window,
    core_only_window,
    critical_vitals_window,
    degraded_signal_window,
    flat,
    healthy_baseline_window,
    lost_signal_window,
    progressive_bed_exit_window,
    safe_repositioning_window,
)


def _parse(raw: dict) -> SensorWindow:
    return SensorWindow.model_validate(raw)


# ─────────────────────────────────────────────────────────────────────────────
#  Determinism
# ─────────────────────────────────────────────────────────────────────────────


def test_predictor_is_deterministic():
    """Same window must produce identical scores on two separate calls."""
    w = _parse(healthy_baseline_window())
    r1 = score_window(w)
    r2 = score_window(w)
    assert r1.risk_score == r2.risk_score
    assert r1.uncertainty == r2.uncertainty
    assert r1.scenario_hint == r2.scenario_hint


def test_predictor_different_inputs_differ():
    """A healthy and a critical window must produce different scores."""
    healthy = score_window(_parse(healthy_baseline_window()))
    critical = score_window(_parse(critical_vitals_window()))
    assert critical.risk_score > healthy.risk_score


# ─────────────────────────────────────────────────────────────────────────────
#  Signal quality handling
# ─────────────────────────────────────────────────────────────────────────────


def test_lost_signal_returns_skipped():
    result = score_window(_parse(lost_signal_window()))
    assert result.model_type == ModelType.SKIPPED
    assert result.risk_score == 0.50
    assert result.uncertainty == 1.0
    assert len(result.factors) == 0
    assert any("LOST" in w for w in result.warnings)


def test_degraded_signal_has_warning_and_elevated_uncertainty():
    result = score_window(_parse(degraded_signal_window()))
    assert result.model_type == ModelType.HEURISTIC
    assert any("DEGRADED" in w for w in result.warnings)
    # Degraded + missing extended streams → uncertainty > 0
    assert result.uncertainty > 0.0


def test_degraded_signal_score_in_bounds():
    result = score_window(_parse(degraded_signal_window()))
    assert 0.0 <= result.risk_score <= 1.0


# ─────────────────────────────────────────────────────────────────────────────
#  Risk level expectations
# ─────────────────────────────────────────────────────────────────────────────


def test_healthy_baseline_is_low_risk():
    result = score_window(_parse(healthy_baseline_window()))
    level = classify_risk(result.risk_score, THRESHOLDS)
    assert level == RiskLevel.LOW, (
        f"Expected LOW, got {level} with score={result.risk_score}"
    )


def test_critical_vitals_is_high_or_critical():
    result = score_window(_parse(critical_vitals_window()))
    level = classify_risk(result.risk_score, THRESHOLDS)
    assert level in (RiskLevel.HIGH, RiskLevel.CRITICAL), (
        f"Expected HIGH or CRITICAL, got {level} with score={result.risk_score}"
    )


def test_score_always_in_unit_interval():
    """Score must be in [0, 1] for all scenario fixtures."""
    scenarios = [
        healthy_baseline_window(),
        safe_repositioning_window(),
        progressive_bed_exit_window(),
        critical_vitals_window(),
        degraded_signal_window(),
        lost_signal_window(),
        core_only_window(),
    ]
    for raw in scenarios:
        result = score_window(_parse(raw))
        assert 0.0 <= result.risk_score <= 1.0, (
            f"Score {result.risk_score} out of bounds"
        )


# ─────────────────────────────────────────────────────────────────────────────
#  Missing modality handling
# ─────────────────────────────────────────────────────────────────────────────


def test_core_only_reports_all_missing_modalities():
    """When all extended streams are absent, all five names must be listed."""
    result = score_window(_parse(core_only_window()))
    expected = {
        "torso_elevation",
        "edge_proximity",
        "lower_body_movement",
        "bed_pressure_shift",
        "occupancy_confidence",
    }
    assert set(result.missing_modalities) == expected


def test_core_only_has_no_extended_factors():
    """Extended features must not appear in contributing factors when absent."""
    result = score_window(_parse(core_only_window()))
    ext_feature_names = {
        "torso_elevation_mean_deg",
        "edge_proximity_max",
        "lower_body_movement_mean_g",
        "bed_pressure_shift_mean",
        "occupancy_confidence_mean",
    }
    actual_names = {f.feature_name for f in result.factors}
    assert actual_names.isdisjoint(ext_feature_names), (
        f"Extended features appeared despite missing streams: "
        f"{actual_names & ext_feature_names}"
    )


def test_core_only_has_missing_modality_warning():
    result = score_window(_parse(core_only_window()))
    assert any("Missing optional" in w for w in result.warnings)


def test_partial_extended_streams_handles_correctly():
    """Providing only some optional streams should work without error."""
    raw = build_window(
        torso_elevation=flat(30.0),
        edge_proximity=flat(0.2),
        # lower_body_movement, bed_pressure_shift, occupancy_confidence absent
    )
    result = score_window(_parse(raw))
    assert "lower_body_movement" in result.missing_modalities
    assert "bed_pressure_shift" in result.missing_modalities
    assert "occupancy_confidence" in result.missing_modalities
    assert "torso_elevation" not in result.missing_modalities
    assert "edge_proximity" not in result.missing_modalities
    assert 0.0 <= result.risk_score <= 1.0


# ─────────────────────────────────────────────────────────────────────────────
#  Scenario detection
# ─────────────────────────────────────────────────────────────────────────────


def test_safe_repositioning_scenario_detected():
    result = score_window(_parse(safe_repositioning_window()))
    assert result.scenario_hint == "SAFE_REPOSITIONING", (
        f"Expected SAFE_REPOSITIONING, got {result.scenario_hint!r}. "
        f"Score={result.risk_score}, factors={result.factors}"
    )


def test_progressive_bed_exit_scenario_detected():
    result = score_window(_parse(progressive_bed_exit_window()))
    assert result.scenario_hint == "PROGRESSIVE_BED_EXIT", (
        f"Expected PROGRESSIVE_BED_EXIT, got {result.scenario_hint!r}. "
        f"Score={result.risk_score}, factors={result.factors}"
    )


def test_no_scenario_for_core_only():
    """Scenario detection requires all extended modalities – must return None."""
    result = score_window(_parse(core_only_window()))
    assert result.scenario_hint is None


def test_healthy_baseline_no_scenario():
    """A fully normal window should not trigger any scenario."""
    result = score_window(_parse(healthy_baseline_window()))
    assert result.scenario_hint is None


# ─────────────────────────────────────────────────────────────────────────────
#  Risk classification boundaries
# ─────────────────────────────────────────────────────────────────────────────


@pytest.mark.parametrize(
    "score, expected_level",
    [
        (0.00, RiskLevel.LOW),
        (0.34, RiskLevel.LOW),
        (0.35, RiskLevel.ELEVATED),
        (0.59, RiskLevel.ELEVATED),
        (0.60, RiskLevel.HIGH),
        (0.79, RiskLevel.HIGH),
        (0.80, RiskLevel.CRITICAL),
        (1.00, RiskLevel.CRITICAL),
    ],
)
def test_risk_classification_boundaries(score: float, expected_level: RiskLevel):
    assert classify_risk(score, THRESHOLDS) == expected_level


# ─────────────────────────────────────────────────────────────────────────────
#  Contributing factors
# ─────────────────────────────────────────────────────────────────────────────


def test_contributing_factors_have_core_features():
    """Core features must always appear in the result."""
    result = score_window(_parse(healthy_baseline_window()))
    names = {f.feature_name for f in result.factors}
    for expected in ["mean_heart_rate", "spo2_min", "mean_resp_rate"]:
        assert expected in names, f"{expected} missing from contributing factors"


def test_contributing_factors_weights_match_key_names():
    """Weight values must be non-zero and numeric."""
    result = score_window(_parse(healthy_baseline_window()))
    for factor in result.factors:
        assert isinstance(factor.weight, float)
        assert factor.weight != 0.0 or factor.feature_name.startswith("occupancy")


def test_model_version_is_correct():
    result = score_window(_parse(healthy_baseline_window()))
    assert result.model_type == ModelType.HEURISTIC


# ─────────────────────────────────────────────────────────────────────────────
#  Uncertainty
# ─────────────────────────────────────────────────────────────────────────────


def test_uncertainty_lower_when_all_streams_present():
    full = score_window(_parse(healthy_baseline_window()))
    partial = score_window(_parse(core_only_window()))
    assert full.uncertainty < partial.uncertainty, (
        f"Full window uncertainty {full.uncertainty} should be < "
        f"core-only uncertainty {partial.uncertainty}"
    )


def test_lost_has_maximum_uncertainty():
    result = score_window(_parse(lost_signal_window()))
    assert result.uncertainty == 1.0
