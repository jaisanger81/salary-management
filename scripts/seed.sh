#!/usr/bin/env bash
set -euo pipefail

DATABASE_URL="${DATABASE_URL:-postgresql://salary:salary@localhost:5432/salary_management}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required. Install the PostgreSQL client or run: docker compose exec -T db psql -U salary -d salary_management -f /dev/stdin < scripts/seed.sql" >&2
  exit 1
fi

psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$SCRIPT_DIR/seed.sql"
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -c "SELECT count(*) AS seeded_employees FROM employee;"
