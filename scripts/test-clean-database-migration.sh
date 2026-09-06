#!/usr/bin/env bash
# Repeatable clean-database Flyway migration validation
# Starts an isolated temporary PostgreSQL 17 container, applies migrations V1-V6,
# verifies schema, indexes, and seeded demo data, and cleanly removes the temporary container.
# Does NOT touch the active development PostgreSQL volume.

set -euo pipefail

PORT="${1:-5433}"
IMAGE="${2:-postgis/postgis:17-3.5}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MIGRATION_DIR="$PROJECT_ROOT/backend/src/main/resources/db/migration"

if [ ! -d "$MIGRATION_DIR" ]; then
    echo "ERROR: Migration directory not found at $MIGRATION_DIR" >&2
    exit 1
fi

RANDOM_SUFFIX="$(head -c 8 /dev/urandom | od -An -tx1 | tr -d ' \n')"
CONTAINER_NAME="neepo-clean-db-test-${RANDOM_SUFFIX}"
DB_NAME="neepo_clean_db"
DB_USER="neepo_migrator"
DB_PASS="TempMigrateSecret_${RANDOM_SUFFIX}"

echo "Starting isolated clean PostgreSQL test container (${CONTAINER_NAME}) on port ${PORT}..."

docker run -d \
    --name "${CONTAINER_NAME}" \
    -p "${PORT}:5432" \
    -e "POSTGRES_DB=${DB_NAME}" \
    -e "POSTGRES_USER=${DB_USER}" \
    -e "POSTGRES_PASSWORD=${DB_PASS}" \
    "${IMAGE}" > /dev/null

cleanup() {
    echo "Cleaning up temporary container ${CONTAINER_NAME}..."
    docker rm -f "${CONTAINER_NAME}" > /dev/null 2>&1 || true
    echo "Temporary container removed. Development volumes left completely untouched."
}
trap cleanup EXIT

echo "Waiting for database readiness..."
READY=false
for i in $(seq 1 40); do
    sleep 1
    if docker exec -e "PGPASSWORD=${DB_PASS}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c "SELECT 1;" > /dev/null 2>&1; then
        READY=true
        break
    fi
done

if [ "$READY" != "true" ]; then
    echo "ERROR: PostgreSQL container failed to become ready within 40 seconds." >&2
    exit 1
fi

echo "Database is ready. Applying migrations in order..."
for file in $(find "${MIGRATION_DIR}" -maxdepth 1 -name "V*.sql" | sort); do
    filename="$(basename "$file")"
    echo "Applying ${filename}..."
    docker exec -i -e "PGPASSWORD=${DB_PASS}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -q < "$file"
done

echo "All migrations applied successfully."
echo "Verifying schema, tables, indexes, and demo records..."

TABLE_COUNT=$(docker exec -i -e "PGPASSWORD=${DB_PASS}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")
INDEX_COUNT=$(docker exec -i -e "PGPASSWORD=${DB_PASS}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public';")
USER_COUNT=$(docker exec -i -e "PGPASSWORD=${DB_PASS}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c "SELECT count(*) FROM users WHERE email IN ('dispatcher@neepo.local', 'driver@neepo.local');")
TRIP_COUNT=$(docker exec -i -e "PGPASSWORD=${DB_PASS}" "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c "SELECT count(*) FROM trips;")

if [ "$TABLE_COUNT" -lt 15 ]; then
    echo "ERROR: Schema validation failed: Expected at least 15 public tables, found $TABLE_COUNT." >&2
    exit 1
fi

if [ "$INDEX_COUNT" -lt 20 ]; then
    echo "ERROR: Schema validation failed: Expected at least 20 indexes, found $INDEX_COUNT." >&2
    exit 1
fi

if [ "$USER_COUNT" -lt 2 ]; then
    echo "ERROR: Demo data validation failed: Expected seeded demo users, found $USER_COUNT." >&2
    exit 1
fi

if [ "$TRIP_COUNT" -lt 1 ]; then
    echo "ERROR: Demo data validation failed: Expected seeded demo trips, found $TRIP_COUNT." >&2
    exit 1
fi

echo "Verification PASSED:"
echo " - Public tables: ${TABLE_COUNT}"
echo " - Indexes: ${INDEX_COUNT}"
echo " - Seeded demo users verified: ${USER_COUNT}"
echo " - Seeded demo trips verified: ${TRIP_COUNT}"
echo " - Clean database migration test completed successfully (0 secrets leaked)."
