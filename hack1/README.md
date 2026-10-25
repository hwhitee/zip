# WardSentinel AI

**Privacy-preserving clinical decision-support prototype for predictive ward safety using synthetic ambient sensor data.**

> ⚠️ All patient data in this system is **fully synthetic**. No real PHI is ever used or stored.

---

## Architecture

```
┌─────────────┐   WebSocket / REST   ┌──────────────┐
│  Next.js 15 │◄────────────────────►│ Spring Boot 3│
│  (Frontend) │                      │  (Backend)   │
└─────────────┘                      └──────┬───────┘
                                            │ HTTP (internal)
                                     ┌──────▼───────┐
                                     │  FastAPI      │
                                     │  (Inference)  │
                                     └──────┬───────┘
                                            │
                                     ┌──────▼───────┐
                                     │  PostgreSQL   │
                                     │  + Flyway     │
                                     └──────────────┘
```

| Service | Language | Port |
|---------|----------|------|
| Frontend | Next.js 15 + TypeScript | 3000 |
| Backend API | Java 21 + Spring Boot 3 | 8080 |
| Inference | Python 3.11+ + FastAPI | 8001 |
| Database | PostgreSQL 16 | 5432 |

---

## Quick Start (Docker)

**Prerequisites:** Docker Desktop, Node.js ≥ 20, Java 21 JDK, Maven 3.9+, Python 3.11+

```bash
# 1. Clone & enter
git clone <repo-url> wardsentinel && cd wardsentinel

# 2. Copy secrets template
cp infra/.env.example infra/.env
# Edit infra/.env — add GEMINI_API_KEY if you want AI explanations

# 3. Start infrastructure (PostgreSQL + pgAdmin)
docker compose -f infra/docker-compose.yml up -d

# 4. Start inference service
cd inference && pip install -r requirements.txt && uvicorn app.main:app --port 8001 --reload

# 5. Start backend (new terminal)
cd backend && mvn spring-boot:run

# 6. Start frontend (new terminal)
cd frontend && npm install && npm run dev

# 7. Open dashboard
open http://localhost:3000/dashboard
```

---

## Monorepo Layout

```
hack1/
├── docs/           Architecture decisions, API contracts, data dictionary
├── infra/          Docker Compose, .env.example, DB init
├── db/migrations/  Flyway SQL migrations (source of truth for schema)
├── frontend/       Next.js 15 + React 19 + TypeScript + Tailwind
├── backend/        Java 21 + Spring Boot 3 REST + WebSocket gateway
└── inference/      Python 3.11 + FastAPI + NumPy scoring engine
```

---

## Key Concepts

| Concept | Description |
|---------|-------------|
| **SensorWindow** | 5-minute sliding window of 7 ambient sensor streams per patient |
| **InferenceResult** | Risk score (0–1), risk level, contributing factors, optional Gemini explanation |
| **SafetyEvent** | Stateful alert (DETECTED → ACKNOWLEDGED → RESOLVED) triggered when risk ≥ HIGH |
| **Heuristic Predictor** | NumPy z-score ensemble — no ML model training needed for hackathon |

---

## Risk Levels

| Level | Score Range | Action |
|-------|-------------|--------|
| LOW | 0.00–0.34 | No alert |
| ELEVATED | 0.35–0.59 | Passive dashboard highlight |
| HIGH | 0.60–0.79 | SafetyEvent created, nurse notified via WebSocket |
| CRITICAL | 0.80–1.00 | SafetyEvent auto-escalated after 2 minutes |

---

## Development Milestones

| Milestone | Description | Status |
|-----------|-------------|--------|
| M0 | Foundation: git, infra, DB migrations, Next.js scaffold | 🔄 In Progress |
| M1 | Inference service: FastAPI + heuristic predictor | ⬜ Pending |
| M2 | Spring Boot backend: REST + WebSocket + Flyway | ⬜ Pending |
| M3 | Frontend dashboard: WardMap + live alerts | ⬜ Pending |
| M4 | Synthetic data simulator | ⬜ Pending |
| M5 | Integration, polish, demo | ⬜ Pending |

---

## Environment Variables

See [`infra/.env.example`](infra/.env.example) for all required variables.

The only **optional** variable is `GEMINI_API_KEY` — the system degrades gracefully without it (explanations are omitted).

---

## License

MIT — hackathon prototype, not for clinical use.
