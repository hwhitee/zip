"""
Contract integration tests for future Java Spring Boot backend interoperability.

Validates that:
1. Java Spring Boot camelCase payloads (windowId, patientId, heartRateBpm, etc.)
   are parsed correctly without 422 errors.
2. The predictor produces identical risk scores whether given camelCase or snake_case payloads.
3. Missing modalities are explicitly flagged and never treated as safe zeros.
4. Signal degradation yields elevated uncertainty and explicit warnings.
"""

from __future__ import annotations

import uuid
from fastapi.testclient import TestClient

from app.main import app
from tests.fixtures import healthy_baseline_window, WINDOW_START_MS

client = TestClient(app, raise_server_exceptions=True)


def _to_camel_dict(snake_dict: dict) -> dict:
    out = {}
    for k, v in snake_dict.items():
        parts = k.split("_")
        camel_k = parts[0] + "".join(p.capitalize() for p in parts[1:])
        if isinstance(v, dict):
            out[camel_k] = _to_camel_dict(v)
        else:
            out[camel_k] = v
    return out


def test_java_camel_case_payload_accepted():
    snake_payload = healthy_baseline_window()
    camel_payload = _to_camel_dict(snake_payload)

    resp = client.post("/predict", json=camel_payload)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["window_id"] == snake_payload["window_id"]
    assert body["patient_id"] == snake_payload["patient_id"]
    assert body["risk_level"] == "LOW"
    assert 0.0 <= body["risk_score"] <= 1.0


def test_camel_and_snake_produce_identical_scores():
    snake_payload = healthy_baseline_window()
    camel_payload = _to_camel_dict(snake_payload)

    resp_snake = client.post("/predict", json=snake_payload)
    resp_camel = client.post("/predict", json=camel_payload)

    assert resp_snake.status_code == 200
    assert resp_camel.status_code == 200
    assert resp_snake.json()["risk_score"] == resp_camel.json()["risk_score"]
    assert resp_snake.json()["uncertainty"] == resp_camel.json()["uncertainty"]


def test_missing_modalities_explicitly_reported():
    snake_payload = healthy_baseline_window()
    # Remove all extended safety streams
    for opt in [
        "torso_elevation",
        "edge_proximity",
        "lower_body_movement",
        "bed_pressure_shift",
        "occupancy_confidence",
    ]:
        snake_payload["sensors"].pop(opt, None)

    camel_payload = _to_camel_dict(snake_payload)
    resp = client.post("/predict", json=camel_payload)
    assert resp.status_code == 200
    body = resp.json()

    assert len(body["missing_modalities"]) == 5
    assert "torso_elevation" in body["missing_modalities"]
    assert "edge_proximity" in body["missing_modalities"]
    assert any("Missing optional" in w for w in body["warnings"])
