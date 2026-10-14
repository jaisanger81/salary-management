# ACME Salary Management

A learning/demo application for an HR manager to search a fictional 10,000-person workforce, review salary data by currency, and record salary changes. The backend uses **Java 21+, Quarkus 3.40.1, Apache Camel Quarkus, PostgreSQL, and Flyway**. The UI uses **React, TypeScript, and Vite**.

> **Demo only:** authentication is not implemented. Use only synthetic data. Do not expose this application or enter real employee salary information.

## Architecture at a glance

```text
React (localhost:5173)
   │ JSON /api/v1/*
   ▼
Quarkus REST resources → Camel direct routes → application service → PostgreSQL
                                                        └── atomic salary + audit transaction
```

See [`REQUIREMENTS.md`](REQUIREMENTS.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), and [`docs/EXPLANATION.md`](docs/EXPLANATION.md) before exploring the implementation. The architecture diagram and reasoning are written for interview discussion.

## Prerequisites

- JDK 21 or later, Maven 3.9+, Node.js 20+ (Vite 8), and npm.
- Docker Compose **or** a local PostgreSQL 17 database.

## Start locally

From the project root:

1. Start the database: `docker compose up -d db`. Wait until `docker compose ps` reports the database healthy.
2. Start the API in a terminal: `mvn quarkus:dev`. Flyway creates the schema on first startup. API: <http://localhost:8080>; health: <http://localhost:8080/q/health>.
3. In another terminal: `cd frontend && npm install && npm run dev`. UI: <http://localhost:5173>.
4. In a third terminal, seed demo data with the PostgreSQL client bundled in the database container:

   ```bash
   docker compose exec -T db psql -U salary -d salary_management -v ON_ERROR_STOP=1 < scripts/seed.sql
   docker compose exec -T db psql -U salary -d salary_management -c 'SELECT count(*) FROM employee;'
   ```

   Alternatively, install the `psql` client and run `./scripts/seed.sh`. Override its connection with `DATABASE_URL=postgresql://user:pass@host:5432/db ./scripts/seed.sh`.

The seed uses fixed IDs and employee numbers and skips existing employee numbers, so it is safe to rerun. It inserts 10,000 fictional employees. Check the count shown by the script or the dashboard.

## Run checks

```bash
mvn test
cd frontend && npm test -- --run && npm run build
```

Backend tests that exercise database-backed API behavior require PostgreSQL. The Quarkus test configuration uses the local Compose database when available; start it first with `docker compose up -d db`.

## Useful API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/employees?query=&country=&department=&page=0&size=25` | Search/filter and paginate |
| `GET` | `/api/v1/employees/{id}` | Employee detail |
| `GET` | `/api/v1/employees/{id}/salary-history` | Salary change audit trail |
| `PUT` | `/api/v1/employees/{id}/salary` | Update annual salary and currency |
| `GET` | `/api/v1/summary` | Headcount and salary totals grouped by currency |
| `GET` | `/api/v1/metadata` | Country and department filters |

Paging is zero-based and the maximum page size is 100. Salary amounts use decimal precision; totals are never combined across currencies.

## Configuration

Backend defaults are suitable for local Compose (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ORIGINS`). In a real deployment, use a secret manager, strong credentials, TLS, and a strict CORS origin. Do not use the demo defaults in a shared environment.

The Vite dev server proxies `/api` to `http://localhost:8080`; set `VITE_API_URL` to an API base URL if serving the UI separately. Set the same `VITE_API_URL` in the UI build environment when making a production build.

## Learn / explain the code

Start with `docs/EXPLANATION.md` for a short interview walkthrough and end-to-end salary update flow. Review `docs/AI_WORKFLOW.md` and fill it with your actual AI tool/model and prompts; it is intentionally a truthful template, not a fabricated conversation. Add incremental commits in your own Git repository as you develop and verify the project.
