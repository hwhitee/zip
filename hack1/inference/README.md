# WardSentinel Inference Service

**Heuristic ambient-sensor risk scoring service for predictive ward safety.**

> ⚠️ **DISCLAIMER: DEMO ONLY — NOT CLINICALLY VALIDATED**
> Risk scores produced by this service are heuristic demonstration values derived from synthetic ambient sensor readings using deterministic hand-tuned rules and z-score components.
> They are **NOT** calibrated clinical probabilities, have **NOT** been trained on actual patient health data, and must **NOT** be used for clinical decision-making.

---

## Capabilities & Architecture

- **Validated Sensor Window Ingestion**: Validates fixed 5-minute windows (default 300 samples at 1.0 Hz) ensuring strict timestamp ordering, uniform sensor length, physiological value bounds, and signal quality metadata.
- **Deterministic Temporal Baselines**: Evaluates core vitals (`heart_rate_bpm`, `spo2_pct`, `resp_rate_breaths`, `skin_temp_c`, `motion_g`) and extended ambient safety streams:
  - `torso_elevation` (degrees horizontal, trend slope)
  - `edge_proximity` (distance from edge, 0=center, 1=edge)
  - `lower_body_movement` (accelerometer magnitude, variance)
  - `bed_pressure_shift` (redistribution vs. sustained pressure)
  - `occupancy_confidence` (bed presence probability)
- **Heuristic Demo Risk Score (0.00000 - 1.00000)**: Normalised risk score classified into `LOW`, `ELEVATED`, `HIGH`, and `CRITICAL`.
- **Uncertainty & Explicit Missing Modality Handling**:
  - Missing sensor channels are **never silently replaced with safe zero values**. They are excluded from active weight normalisation, registered in `missing_modalities`, surfaced in `warnings`, and proportionately elevate `uncertainty`.
  - Degraded signal mode (`DEGRADED`) adds diagnostic warnings and increases uncertainty.
  - Lost signal (`LOST`) triggers safe mode: skips computation, returns fixed neutral score `0.50`, maximum uncertainty `1.0`, and sets `model_type="SKIPPED"`.
- **Deterministic Demo Scenarios**:
  - `SAFE_REPOSITIONING`: Nurse-assisted repositioning pattern (moderate torso elevation, high bed pressure shift, low edge proximity).
  - `PROGRESSIVE_BED_EXIT`: Pre-fall / bed-exit trajectory (rising edge proximity, active lower body movement, declining occupancy, rising torso elevation).
- **Optional Gemini Explanations**: Non-blocking natural language clinical explanations powered by `google-genai` and `gemini-3.8-flash` when `GEMINI_API_KEY` is provided.

---

## Project Structure

```
inference/
├── app/
│   ├── main.py                  # FastAPI application entry point, lifecycle, error handlers
│   ├── models/
│   │   ├── schemas.py           # Pydantic v2 data models matching docs/api-contracts.md
│   │   └── predictor.py         # Deterministic NumPy scoring pipeline and scenario detector
│   ├── routers/
│   │   └── predict.py           # /predict, /health, /model/info endpoints
│   └── services/
│       └── gemini_explain.py    # Non-blocking optional Gemini explanation integration
├── tests/
│   ├── fixtures.py              # Synthetic deterministic test fixtures (all scenarios)
│   ├── test_schemas.py          # Boundary validation & contract conformance tests
│   ├── test_predictor.py        # Heuristic scoring, missing modalities, and scenario tests
│   └── test_api.py              # End-to-end FastAPI TestClient integration tests
├── requirements.txt
├── pyproject.toml
└── README.md
```

---

## Setup & Execution

### 1. Prerequisites
- Python 3.11+
- Virtual environment or package manager

### 2. Installation
From the repository root or inside `inference/`:

```powershell
cd inference
python -m pip install -r requirements.txt
```

### 3. Run Test Suite
Execute the comprehensive test suite with verbose reporting:

```powershell
python -m pytest tests/ -v
```

All 67 tests cover schema validation, contract bounds, missing modality warnings, signal quality transitions, scenario matching, and REST API responses.

### 4. Start the Service Locally

```powershell
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

The interactive OpenAPI documentation is available at `http://localhost:8001/docs`.

---

## API Endpoints

### `POST /predict`
Evaluates a 5-minute ambient sensor window.
- **Request Body**: `SensorWindow` JSON (matches contract in `docs/api-contracts.md`)
- **Response**: `InferenceResult` JSON containing `risk_score`, `risk_level`, `uncertainty`, `contributing_factors`, `missing_modalities`, `warnings`, `scenario_hint`, and `model_version`.

### `GET /health`
Liveness check returning service status, model version, and uptime.

### `GET /model/info`
Returns model type (`HEURISTIC`), core/extended feature lists, and the non-clinical disclaimer.
