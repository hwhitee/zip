# WardSentinel — API Contracts

> **Canonical reference.** All services must conform to the types defined here.
> Generated types live in `frontend/src/lib/types.ts` (TypeScript) and `inference/app/models/schemas.py` (Pydantic).

---

## 1. SensorWindow  (Producer → Backend → Inference)

```typescript
interface SensorPayload {
  heartRateBpm:      number[];  // beats per minute
  spo2Pct:           number[];  // peripheral O2 saturation %
  respRateBreaths:   number[];  // breaths per minute
  skinTempC:         number[];  // degrees Celsius
  motionG:           number[];  // accelerometer magnitude (g)
  ambientNoiseDb:    number[];  // dB SPL
  lightLux:          number[];  // lux
}

// All arrays have length = Math.round(samplingHz * windowDurationSeconds)
// Default: 1.0 Hz × 300 s = 300 samples per array

interface SensorWindowMetadata {
  deviceId:       string;
  firmwareVer:    string;
  signalQuality:  "GOOD" | "DEGRADED" | "LOST";
}

interface SensorWindow {
  windowId:        string;    // UUID v4 — producer-assigned, idempotent
  patientId:       string;    // UUID v4
  wardId:          string;    // e.g. "WARD-4B"
  bedLabel:        string;    // e.g. "04"
  startEpochMs:    number;    // inclusive, Unix ms
  endEpochMs:      number;    // exclusive, Unix ms (endEpochMs - startEpochMs = 300 000)
  samplingHz:      number;    // default 1.0
  schemaVersion?:  string;    // default "1.0.0"
  sequenceNumber?: number;    // monotonic counter
  idempotencyKey?: string;    // deduplication key
  sensors:         SensorPayload;
  metadata:        SensorWindowMetadata;
}
```

**Validation & Safety rules:**
- `endEpochMs - startEpochMs` must equal `windowDuration * 1000` (default 300 000 ms = 5 minutes).
- All sensor arrays must have equal length matching `samplingHz * 300`.
- Missing extended streams are recorded in `missing_modalities` and proportionately elevate `uncertainty`.
- `signalQuality = "LOST"` → triggers deterministic fail-safe HIGH safety event.
- Python Inference Outage (`modelType = "SKIPPED"`) → `riskScore = null`, triggers operational HIGH safety alert. Never fabricates a safe numeric score.

---

## 2. InferenceResult  (Inference → Backend → Frontend)

```typescript
interface ContributingFactor {
  featureName: string;   // e.g. "mean_heart_rate", "spo2_min"
  value:       number;   // actual computed value
  weight:      number;   // signed importance; positive = raises risk
}

interface RiskThresholds {
  low:      number;  // 0.35
  elevated: number;  // 0.60
  high:     number;  // 0.80
}

type RiskLevel = "LOW" | "ELEVATED" | "HIGH" | "CRITICAL";
type ModelType = "HEURISTIC" | "ML" | "SKIPPED";

interface InferenceResult {
  resultId:    string;   // UUID v4
  windowId:    string;   // echoes the input windowId
  patientId:   string;

  computedAtMs:  number;
  latencyMs:     number;

  riskScore:     number | null; // 0.000–1.000, null when modelType = 'SKIPPED' (never fabricate fake score)
  riskLevel:     RiskLevel;
  riskThresholds: RiskThresholds;

  contributingFactors: ContributingFactor[];

  geminiExplanation?: string | null;   // null when unavailable

  modelVersion: string;   // e.g. "v0.1.0-heuristic"
  modelType:    ModelType;
}
```

**Risk level mapping:**

| Score | Level |
|-------|-------|
| 0.000 – 0.349 | LOW |
| 0.350 – 0.599 | ELEVATED |
| 0.600 – 0.799 | HIGH |
| 0.800 – 1.000 | CRITICAL |

---

## 3. SafetyEvent  (Backend → Frontend via WebSocket)

```typescript
type SafetyEventStatus =
  | "DETECTED"
  | "ACKNOWLEDGED"
  | "INVESTIGATING"
  | "ESCALATED"
  | "RESOLVED"
  | "DISMISSED";

interface AuditNote {
  ts:     string;   // ISO 8601
  author: string;
  text:   string;
}

interface SafetyEvent {
  eventId:             string;
  patientId:           string;
  wardId:              string;
  triggeringResultId:  string | null;
  severity:            RiskLevel;
  status:              SafetyEventStatus;

  detectedAtMs:        number;
  acknowledgedAtMs?:   number | null;
  resolvedAtMs?:       number | null;

  assignedTo?:         string | null;
  notes:               AuditNote[];
  autoEscalateAfterMs?: number | null;
}
```

**Valid status transitions:**

```
DETECTED → ACKNOWLEDGED → INVESTIGATING → RESOLVED
                                       → ESCALATED → RESOLVED
DETECTED → DISMISSED
```

Attempting an invalid transition returns `HTTP 409 Conflict`.

---

## 4. REST API — Spring Boot (port 8080)

### Base URL: `http://localhost:8080/api/v1`

#### Patients

| Method | Path | Request Body | Response | Notes |
|--------|------|-------------|----------|-------|
| `GET` | `/patients` | — | `Patient[]` | Filter: `?wardId=WARD-4B&status=ACTIVE` |
| `GET` | `/patients/{id}` | — | `Patient` | 404 if not found |

#### Sensor Windows

| Method | Path | Request Body | Response | Notes |
|--------|------|-------------|----------|-------|
| `POST` | `/sensor-windows` | `SensorWindow` | `201 InferenceResult` | Stores window, triggers inference synchronously |
| `GET` | `/sensor-windows/{id}` | — | `SensorWindow` | |

#### Inference Results

| Method | Path | Request Body | Response | Notes |
|--------|------|-------------|----------|-------|
| `GET` | `/inference-results` | — | `InferenceResult[]` | Filter: `?patientId=&riskLevel=HIGH&limit=50` |
| `GET` | `/inference-results/{id}` | — | `InferenceResult` | |
| `GET` | `/inference-results/latest` | — | `Map<patientId, InferenceResult>` | One result per active patient |

#### Safety Events

| Method | Path | Request Body | Response | Notes |
|--------|------|-------------|----------|-------|
| `GET` | `/safety-events` | — | `SafetyEvent[]` | Filter: `?wardId=&status=DETECTED` |
| `GET` | `/safety-events/{id}` | — | `SafetyEvent` | |
| `POST` | `/safety-events/{id}/acknowledge` | `{ assignedTo: string }` | `SafetyEvent` | 409 if invalid transition |
| `POST` | `/safety-events/{id}/investigate` | `{ note: string }` | `SafetyEvent` | |
| `POST` | `/safety-events/{id}/escalate` | `{ note: string }` | `SafetyEvent` | |
| `POST` | `/safety-events/{id}/resolve` | `{ note: string }` | `SafetyEvent` | |
| `POST` | `/safety-events/{id}/dismiss` | `{ reason: string }` | `SafetyEvent` | |

#### Health

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/health` | `{ status: "UP", services: { db, inference } }` |

---

## 5. WebSocket API — STOMP over SockJS (port 8080)

**Endpoint:** `ws://localhost:8080/ws`

### Subscriptions (client → server)

| Topic | Payload | Description |
|-------|---------|-------------|
| `/topic/ward/{wardId}/risk` | `InferenceResult` | New risk score for any patient in the ward |
| `/topic/ward/{wardId}/alerts` | `SafetyEvent` | New or updated safety event in the ward |
| `/topic/patient/{patientId}/risk` | `InferenceResult` | Single-patient risk stream |

### Sends (client → server)

| Destination | Payload | Description |
|-------------|---------|-------------|
| `/app/event/acknowledge` | `{ eventId, assignedTo }` | Acknowledge an event via WS (mirrors REST) |

---

## 6. Inference Service REST — FastAPI (port 8001, internal)

> This service is **not publicly exposed**. Only Spring Boot calls it.

| Method | Path | Request Body | Response |
|--------|------|-------------|----------|
| `POST` | `/predict` | `SensorWindow` (Pydantic) | `InferenceResult` |
| `GET` | `/health` | — | `{ status: "ok", model_version: str }` |
| `GET` | `/model/info` | — | `{ version, type, features: str[] }` |

---

## 7. Error Response Shape

All services use this uniform error envelope:

```typescript
interface ApiError {
  timestamp:  string;    // ISO 8601
  status:     number;    // HTTP status code
  error:      string;    // HTTP status phrase
  message:    string;    // human-readable detail
  path:       string;    // request path
  traceId?:   string;    // for correlation
}
```

---

## 8. Versioning Policy

- **URL versioning** (`/api/v1/`) for REST endpoints
- **Breaking changes** always bump the version (`/api/v2/`)
- WebSocket topics include the version implicitly via endpoint path
- Inference service is internal — versioned via `modelVersion` field, not URL
