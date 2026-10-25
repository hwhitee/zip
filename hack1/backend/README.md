# WardSentinel AI — Java Spring Boot 3 Backend

Clinical orchestration gateway, deterministic safety policy engine, nurse assignment dispatcher, transactional outbox, and auditable privacy ledger.

---

## 1. Prerequisites
- **Java 21 JDK** (e.g. OpenJDK 21)
- **Maven 3.9+**
- **PostgreSQL 16** (or Docker)
- **Python 3.11+** with the WardSentinel inference service running on port `8001`

---

## 2. Configuration (`src/main/resources/application.yml`)

Configure environment variables or copy from `hack1/infra/.env.example`:

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `wardsentinel` | Database name |
| `POSTGRES_USER` | `wardsentinel` | Database user |
| `POSTGRES_PASSWORD` | `changeme_dev_only` | Database password |
| `INFERENCE_SERVICE_URL` | `http://localhost:8001` | URL of the Python FastAPI scoring service |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed origins for web client |

---

## 3. Running Locally

### Step 1: Start PostgreSQL (Docker)
```bash
docker compose -f hack1/infra/docker-compose.yml up -d
```

### Step 2: Start the Python Inference Engine (Port 8001)
```bash
cd hack1/inference
source .venv/bin/activate
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### Step 3: Run Backend Tests
```bash
mvn -f hack1/backend/pom.xml test
```

### Step 4: Start the Spring Boot Service (Port 8080)
```bash
mvn -f hack1/backend/pom.xml spring-boot:run
```

---

## 4. Endpoints Implemented

### Telemetry Ingestion
- `POST /api/v1/telemetry/windows`: Ingests a 5-minute derived sensor window, calls inference, runs deterministic safety policy, dispatches nurse, creates outbox event, and records privacy ledger entry.

### Ward & Bed Overview
- `GET /api/v1/wards/{wardId}/overview`: Real-time ward summary with total beds, occupancy count, active/critical alerts, staff count, and per-bed statuses.
- `GET /api/v1/beds`: List all ward beds and coordinates.

### Safety Events Lifecycle FSM
- `GET /api/v1/events`: List events (filtered by `wardId` and `status`).
- `GET /api/v1/events/{id}`: Detailed event record.
- `GET /api/v1/events/{id}/evidence`: Triggering inference result and contributing feature breakdown.
- `POST /api/v1/events/{id}/acknowledge`: Nurse acknowledgement.
- `POST /api/v1/events/{id}/investigate`: Status update to `INVESTIGATING`.
- `POST /api/v1/events/{id}/escalate`: Escalates to charge nurse or physician.
- `POST /api/v1/events/{id}/resolve`: Closes the alert after bedside intervention.
- `POST /api/v1/events/{id}/dismiss`: False-positive override.
- `POST /api/v1/events/{id}/assign`: Reassigns nurse.

### Nursing Staff
- `GET /api/v1/staff/availability`: Real-time on-shift, available staff and active assignment counts.

### Auditable Privacy Ledger
- `GET /api/v1/privacy/ledger`: Chronological audit entries.
- `POST /api/v1/privacy/ledger/verify`: Cryptographically verifies the SHA-256 hash-chain integrity.

### WebSocket / STOMP
- Endpoint: `ws://localhost:8080/ws` (SockJS fallback enabled)
- Topics:
  - `/topic/ward/{wardId}/alerts`: Dispatches `SafetyEvent` creations and status transitions.
  - `/topic/ward/{wardId}/risk`: Real-time risk score stream for all ward patients.
  - `/topic/patient/{patientId}/risk`: Dedicated single-patient risk stream.
