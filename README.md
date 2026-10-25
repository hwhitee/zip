# WardSentinel AI — Clinical Ambient Safety & Decision-Support

WardSentinel AI is a privacy-preserving ambient AI prototype for predictive ward safety and unobserved fall-risk detection. It processes synthetic sensor data, calculates demo risk scores, applies deterministic safety policies, dispatches nurse alerts, displays real-time ward events, and maintains an auditable privacy ledger.

> **DISCLAIMER:** This application is a hackathon demonstration prototype using hand-tuned rules on synthetic data. It is **not** a calibrated clinical probability tool and has **not** undergone clinical trials. It must not be used for real clinical medical decision-making.

---

## Deploying to Vercel

The frontend is a modern **React 19 + TypeScript + Vite + Tailwind CSS** single-page application and is pre-configured for one-click Vercel deployment.

### Option 1: Vercel Web Dashboard (Import Git Repository)
1. Push this repository to GitHub, GitLab, or Bitbucket.
2. Go to [vercel.com/new](https://vercel.com/new) and import the repository.
3. Vercel automatically detects the project using [`vercel.json`](./vercel.json):
   - **Framework Preset:** Vite
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
4. *(Optional)* In **Project Settings → Environment Variables**, configure:
   - `VITE_API_BASE_URL`: The public URL of your deployed Spring Boot backend (e.g., `https://api.yourdomain.com`).
   - If left blank or unset, the application automatically runs in **Offline Demo Mode** with full interactive scenario simulation.
5. Click **Deploy**.

### Option 2: Vercel CLI
```bash
npm i -g vercel
vercel
```
Follow the CLI prompts to deploy.

---

## Project Structure

```
├── vercel.json                 # Vercel deployment configuration (Vite preset & SPA routing)
├── .vercelignore               # Excludes backend/python build artifacts from Vercel uploads
├── vite.config.ts              # Vite 8 configuration with Tailwind CSS and local proxy
├── package.json                # Frontend dependencies (@stomp/stompjs, sockjs-client, lucide-react)
├── index.html                  # HTML entry point
├── src/                        # React SPA Source Code
│   ├── App.tsx                 # Main application shell with live vs. offline state tracking
│   ├── components/             # WardOverviewGrid, BedCard, SafetyEventQueue, PrivacyLedgerModal, etc.
│   ├── services/               # REST API client, scenarios builder, STOMP WebSocket client
│   └── types/                  # TypeScript contract interfaces matching backend
└── hack1/                      # Backend & Microservice Architecture
    ├── backend/                # Java 21 + Spring Boot 3.3.4 (REST Gateway, Policy Engine, WebSocket)
    ├── inference/              # Python 3.11+ + FastAPI (Deterministic Heuristic Scoring Engine)
    ├── db/migrations/          # Flyway PostgreSQL migrations (V1 to V9)
    ├── docs/                   # Shared API contracts and JSON fixtures
    └── infra/                  # Docker Compose (PostgreSQL 16 + pgAdmin)
```

---

## Local Development

### 1. Frontend Only (Offline Demo)
```bash
npm install
npm run dev
```
Open [http://localhost:3000](http://localhost:3000). The app will run in offline demo mode with full scenario testing.

### 2. Full Multi-Tier Stack
- **Database:** `docker compose -f hack1/infra/docker-compose.yml up -d`
- **Inference Service (Port 8001):** `cd hack1/inference && uvicorn app.main:app --port 8001 --reload`
- **Backend (Port 8080):** `cd hack1/backend && mvn spring-boot:run`
- **Frontend (Port 3000):** `npm run dev`
