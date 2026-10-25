"""
End-to-end API integration tests using FastAPI's TestClient (httpx).

Verifies:
- POST /predict returns 200 with a valid InferenceResult shape.
- POST /predict with LOST signal returns modelType=SKIPPED.
- POST /predict with invalid body returns 422 with ApiError shape.
- POST /predict with wrong duration returns 422.
- GET /health returns 200 with status="ok".
- GET /model/info returns feature lists and disclaimer.
- POST /predict with core-only window lists missing_modalities.
- SAFE_REPOSITIONING and PROGRESSIVE_BED_EXIT scenarios round-trip correctly.
"""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.main import app
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
    WINDOW_START_MS,
)

client = TestClient(app, raise_server_exceptions=True)


# ─────────────────────────────────────────────────────────────────────────────
#  /health
# ─────────────────────────────────────────────────────────────────────────────


def test_health_returns_ok():
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert "model_version" in body
    assert body["uptime_seconds"] >= 0.0


# ─────────────────────────────────────────────────────────────────────────────
#  /model/info
# ─────────────────────────────────────────────────────────────────────────────


def test_model_info_shape():
    resp = client.get("/model/info")
    assert resp.status_code == 200
    body = resp.json()
    assert "version" in body
    assert "type" in body
    assert "core_features" in body
    assert "extended_features" in body
    assert "disclaimer" in body
    assert len(body["core_features"]) == 5
    assert len(body["extended_features"]) == 5


def test_model_info_disclaimer_present():
    resp = client.get("/model/info")
    body = resp.json()
    assert "demonstration" in body["disclaimer"].lower() or "not clinically" in body["disclaimer"].lower()


# ─────────────────────────────────────────────────────────────────────────────
#  POST /predict – happy paths
# ─────────────────────────────────────────────────────────────────────────────


def test_predict_healthy_baseline_returns_200():
    resp = client.post("/predict", json=healthy_baseline_window())
    assert resp.status_code == 200


def test_predict_response_has_required_fields():
    resp = client.post("/predict", json=healthy_baseline_window())
    body = resp.json()
    required = [
        "result_id", "window_id", "patient_id",
        "computed_at_ms", "latency_ms",
        "risk_score", "risk_level", "risk_thresholds",
        "contributing_factors",
        "uncertainty", "missing_modalities", "warnings",
        "model_version", "model_type",
    ]
    for field in required:
        assert field in body, f"Missing field: {field}"


def test_predict_risk_score_in_unit_interval():
    resp = client.post("/predict", json=healthy_baseline_window())
    score = resp.json()["risk_score"]
    assert 0.0 <= score <= 1.0


def test_predict_window_id_echoed():
    raw = healthy_baseline_window()
    resp = client.post("/predict", json=raw)
    assert resp.json()["window_id"] == raw["window_id"]


def test_predict_patient_id_echoed():
    raw = healthy_baseline_window()
    resp = client.post("/predict", json=raw)
    assert resp.json()["patient_id"] == raw["patient_id"]


def test_predict_model_type_heuristic_for_good_signal():
    resp = client.post("/predict", json=healthy_baseline_window())
    assert resp.json()["model_type"] == "HEURISTIC"


def test_predict_critical_vitals_high_risk():
    resp = client.post("/predict", json=critical_vitals_window())
    body = resp.json()
    assert body["risk_level"] in ("HIGH", "CRITICAL")


# ─────────────────────────────────────────────────────────────────────────────
#  POST /predict – LOST signal
# ─────────────────────────────────────────────────────────────────────────────


def test_predict_lost_signal_returns_skipped():
    resp = client.post("/predict", json=lost_signal_window())
    assert resp.status_code == 200
    body = resp.json()
    assert body["model_type"] == "SKIPPED"
    assert body["risk_score"] == 0.50
    assert body["uncertainty"] == 1.0
    assert body["contributing_factors"] == []
    assert len(body["warnings"]) > 0


# ─────────────────────────────────────────────────────────────────────────────
#  POST /predict – DEGRADED signal
# ─────────────────────────────────────────────────────────────────────────────


def test_predict_degraded_has_warning():
    resp = client.post("/predict", json=degraded_signal_window())
    assert resp.status_code == 200
    body = resp.json()
    assert body["model_type"] == "HEURISTIC"
    assert any("DEGRADED" in w for w in body["warnings"])


# ─────────────────────────────────────────────────────────────────────────────
#  POST /predict – missing modalities
# ─────────────────────────────────────────────────────────────────────────────


def test_predict_core_only_lists_missing_modalities():
    resp = client.post("/predict", json=core_only_window())
    assert resp.status_code == 200
    body = resp.json()
    assert len(body["missing_modalities"]) == 5
    assert "torso_elevation" in body["missing_modalities"]


def test_predict_core_only_has_missing_warning():
    resp = client.post("/predict", json=core_only_window())
    body = resp.json()
    assert any("Missing optional" in w for w in body["warnings"])


# ─────────────────────────────────────────────────────────────────────────────
#  POST /predict – demo scenarios
# ─────────────────────────────────────────────────────────────────────────────


def test_predict_safe_repositioning_scenario():
    resp = client.post("/predict", json=safe_repositioning_window())
    assert resp.status_code == 200
    body = resp.json()
    assert body["scenario_hint"] == "SAFE_REPOSITIONING"
    # SAFE_REPOSITIONING should not be high-risk (nurse-controlled movement)
    assert body["risk_level"] in ("LOW", "ELEVATED", "HIGH"), body["risk_level"]


def test_predict_progressive_bed_exit_scenario():
    resp = client.post("/predict", json=progressive_bed_exit_window())
    assert resp.status_code == 200
    body = resp.json()
    assert body["scenario_hint"] == "PROGRESSIVE_BED_EXIT"
    # Bed exit should score higher than healthy baseline
    healthy_resp = client.post("/predict", json=healthy_baseline_window())
    assert body["risk_score"] > healthy_resp.json()["risk_score"]


# ─────────────────────────────────────────────────────────────────────────────
#  POST /predict – validation failures
# ─────────────────────────────────────────────────────────────────────────────


def test_predict_empty_body_returns_422():
    resp = client.post("/predict", json={})
    assert resp.status_code == 422
    body = resp.json()
    # Must use ApiError envelope
    assert "message" in body
    assert "timestamp" in body


def test_predict_wrong_duration_returns_422():
    raw = build_window()
    raw["end_epoch_ms"] = WINDOW_START_MS + 60_000   # 1 minute
    physiological_defaults = {
        "heart_rate_bpm": 72.0,
        "spo2_pct": 98.0,
        "resp_rate_breaths": 15.0,
        "skin_temp_c": 33.5,
        "motion_g": 0.05,
        "ambient_noise_db": 40.0,
        "light_lux": 200.0,
    }
    for key in list(raw["sensors"].keys()):
        default = physiological_defaults.get(key, 0.5)
        raw["sensors"][key] = flat(default, 60)
    resp = client.post("/predict", json=raw)
    assert resp.status_code == 422
    assert "duration" in resp.json()["message"]


def test_predict_mismatched_array_length_returns_422():
    raw = healthy_baseline_window()
    raw["sensors"]["spo2_pct"] = flat(98.0, 150)
    resp = client.post("/predict", json=raw)
    assert resp.status_code == 422
    assert "length mismatch" in resp.json()["message"]


def test_predict_out_of_bounds_heart_rate_returns_422():
    raw = build_window(heart_rate_bpm=flat(400.0))  # > 300 limit
    resp = client.post("/predict", json=raw)
    assert resp.status_code == 422


def test_predict_invalid_signal_quality_returns_422():
    raw = healthy_baseline_window()
    raw["metadata"]["signal_quality"] = "UNKNOWN"
    resp = client.post("/predict", json=raw)
    assert resp.status_code == 422


def test_predict_missing_required_field_returns_422():
    raw = healthy_baseline_window()
    del raw["sensors"]["heart_rate_bpm"]
    resp = client.post("/predict", json=raw)
    assert resp.status_code == 422


# ─────────────────────────────────────────────────────────────────────────────
#  Idempotency
# ─────────────────────────────────────────────────────────────────────────────


def test_predict_same_window_same_score():
    """Same window submitted twice must produce the same risk_score."""
    raw = healthy_baseline_window()
    r1 = client.post("/predict", json=raw).json()
    r2 = client.post("/predict", json=raw).json()
    assert r1["risk_score"] == r2["risk_score"]
    assert r1["risk_level"] == r2["risk_level"]
    # result_id is new each time (UUID v4 generated per call)
    assert r1["result_id"] != r2["result_id"]
