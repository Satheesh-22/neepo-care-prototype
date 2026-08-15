# Foundation Validation — 2026-08-13

## Passed on the developer Windows machine — 2026-08-15

- Java 21.0.11, Node.js 24.13.0, npm 11.6.2, Python 3.11.9, Git 2.53.0, Docker 29.3.1 and Docker Compose 5.1.0 were detected.
- `mvnw.cmd --batch-mode clean verify` completed successfully with 9 tests, 0 failures and 0 errors.
- Docker built the backend image and started PostgreSQL/PostGIS, Redis and the backend.
- PostgreSQL and Redis became healthy.
- After enabling the Spring Boot 4 Flyway starter, migrations created the schema before Hibernate validation.
- The backend became healthy and `GET /actuator/health` returned `{"groups":["liveness","readiness"],"status":"UP"}`.

## Passed in the correction workspace

- ZIP input integrity check
- Clean final-ZIP extraction, single-root and build-artifact exclusion checks
- XML parsing for `backend/pom.xml`
- YAML parsing for contracts, application profiles, Compose and CI
- OpenAPI 3.1 validation and local `$ref` resolution
- Java syntax parsing for backend plus all shared Java contracts
- Strict TypeScript type-check for the shared frontend contracts
- Cross-language `TripContext` and `PlanEvaluation` field checks
- Exact alignment of all 19 shared enums between YAML, Java and TypeScript
- Exact field alignment for 20 shared Java records and TypeScript interfaces
- OpenAPI field and optionality alignment for eight shared schemas
- Database check-constraint alignment for 18 persisted shared enums
- Shared WebSocket event-name consistency
- PostgreSQL migration syntax parsing after normalizing only unsupported PostGIS type names in the validation parser
- Fixed seed UUID parsing and required demo-record count
- JSON fixture parsing
- Python and shell-script syntax checks
- Git whitespace/error check
- Fresh-repository initialization and five-branch creation smoke test
- Scan confirming removal of MySQL, the hardcoded database password, `ddl-auto=update` and unversioned `/api/users` routes from source

## Artifact-workspace limitations

The artifact correction workspace cannot reach Maven Central or the developer's Docker engine. Maven compilation and live Compose startup therefore rely on the successful developer-machine results recorded above; static foundation validation is rerun before packaging.

Run on a Java 21 machine with normal Maven Central access:

```bash
cd backend
./mvnw --batch-mode clean verify
```

Then run the infrastructure check:

```bash
cp .env.example .env
# Replace placeholder secrets in .env
docker compose config
docker compose up --build -d postgres redis backend
docker compose ps
```

Expected result: all tests pass, Flyway applies migrations V1–V4 to a new PostGIS database, fixed demo records exist, and the backend health endpoint becomes healthy.
