-- ─────────────────────────────────────────────────────────────
--  PostgreSQL init script — runs once on first container start
--  Enables extensions needed by the application.
--  The wardsentinel DB and user are already created by Docker env vars.
-- ─────────────────────────────────────────────────────────────

-- Required for gen_random_uuid() in Flyway migrations
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Useful for full-text search on notes / explanations (future)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
