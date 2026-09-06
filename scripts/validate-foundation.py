#!/usr/bin/env python3
from __future__ import annotations

import os
import re
import sys
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IGNORED_SCAN_PARTS = {
    ".git",
    "target",
    "node_modules",
    "dist",
    "coverage",
    "playwright-report",
    "test-results",
}

REQUIRED_FILES = [
    "CLAUDE.md",
    "README.md",
    "docs/PRE_DEVELOPMENT_PHASES.md",
    "docs/PROTOTYPE_SCOPE.md",
    "docs/setup/DEVELOPMENT_ENVIRONMENT.md",
    "docs/setup/GIT_WORKFLOW.md",
    "contracts/domain-enums.yaml",
    "contracts/explanation-codes.yaml",
    "contracts/openapi.yaml",
    "contracts/care-engine-contracts.yaml",
    "contracts/websocket-events.yaml",
    "contracts/generated/java/com/neepo/contracts/TripContext.java",
    "contracts/generated/java/com/neepo/contracts/PlanEvaluation.java",
    "contracts/generated/typescript/neepo-contracts.ts",
    "database/schema-design.md",
    "database/erd.mmd",
    "database/seed/DEMO_DATA.md",
    "backend/src/main/resources/db/migration/V1__create_identity_foundation.sql",
    "backend/src/main/resources/db/migration/V2__create_operational_domain.sql",
    "backend/src/main/resources/db/migration/V3__create_context_and_decision_domain.sql",
    "backend/src/main/resources/db/migration/V4__seed_repeatable_demo_data.sql",
    ".env.example",
    "docker-compose.yml",
]

EXPECTED_EVENTS = {
    "TELEMETRY_UPDATED",
    "DISRUPTION_DETECTED",
    "PLANS_GENERATED",
    "PLAN_SELECTED",
    "PLAN_INVALIDATED",
    "REPLANNING_STARTED",
    "REPLANNING_COMPLETED",
    "TRIP_STATUS_CHANGED",
}

PLAN_EVALUATION_FIELDS = {
    "planId",
    "feasible",
    "violatedHardConstraints",
    "warnings",
    "softConstraintResults",
    "riskScore",
    "uncertaintyScore",
    "benefitScore",
    "lambdaCoefficient",
    "muCoefficient",
    "finalUtility",
    "confidence",
    "freshnessSummary",
    "explanationCodes",
    "evaluatedAt",
    "evaluatorVersion",
    "inputHash",
}

TRIP_CONTEXT_FIELDS = {
    "trip",
    "vehicle",
    "driver",
    "cargo",
    "route",
    "providers",
    "safeStops",
    "telemetryObservations",
    "networkState",
    "activeDisruptions",
    "currentRecoveryPlan",
    "evaluationTime",
    "contextHash",
}


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def source_files():
    for directory, child_directories, file_names in os.walk(ROOT):
        child_directories[:] = [
            name for name in child_directories if name not in IGNORED_SCAN_PARTS
        ]
        for file_name in file_names:
            yield Path(directory) / file_name


for relative in REQUIRED_FILES:
    path = ROOT / relative
    if not path.is_file() or path.stat().st_size == 0:
        fail(f"required non-empty file is missing: {relative}")

pom_root = ET.parse(ROOT / "backend/pom.xml").getroot()
pom_namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
dependencies = {
    (
        dependency.findtext("m:groupId", default="", namespaces=pom_namespace),
        dependency.findtext("m:artifactId", default="", namespaces=pom_namespace),
    )
    for dependency in pom_root.findall("m:dependencies/m:dependency", pom_namespace)
}
if (
    "org.springframework.boot",
    "spring-boot-starter-flyway",
) not in dependencies:
    fail("Spring Boot 4 Flyway starter is missing from backend/pom.xml")
if ("org.flywaydb", "flyway-core") in dependencies:
    fail("backend/pom.xml must not declare flyway-core directly; use the Spring Boot Flyway starter")

gitignore_lines = {
    line.strip()
    for line in (ROOT / ".gitignore").read_text(encoding="utf-8").splitlines()
    if line.strip() and not line.lstrip().startswith("#")
}
if ".env" not in gitignore_lines:
    fail(".env must be ignored by Git")

enum_text = (ROOT / "contracts/domain-enums.yaml").read_text(encoding="utf-8")
event_text = (ROOT / "contracts/websocket-events.yaml").read_text(encoding="utf-8")
openapi_text = (ROOT / "contracts/openapi.yaml").read_text(encoding="utf-8")
seed_text = (
    ROOT
    / "backend/src/main/resources/db/migration/V4__seed_repeatable_demo_data.sql"
).read_text(encoding="utf-8")

for event_name in sorted(EXPECTED_EVENTS):
    if event_name not in enum_text or event_name not in event_text:
        fail(f"shared WebSocket event is inconsistent: {event_name}")

required_paths = [
    "/auth/login:",
    "/vehicles:",
    "/drivers:",
    "/cargo:",
    "/providers:",
    "/trips:",
    "/telemetry:",
    "/disruptions:",
    "/trips/{tripId}/recovery-plans/generate:",
    "/trips/{tripId}/decision-explanation:",
    "/trips/{tripId}/metrics:",
]
for api_path in required_paths:
    if api_path not in openapi_text:
        fail(f"OpenAPI path is missing: {api_path}")

plan_contracts = [
    openapi_text,
    (ROOT / "contracts/care-engine-contracts.yaml").read_text(encoding="utf-8"),
    (
        ROOT
        / "contracts/generated/java/com/neepo/contracts/PlanEvaluation.java"
    ).read_text(encoding="utf-8"),
    (
        ROOT / "contracts/generated/typescript/neepo-contracts.ts"
    ).read_text(encoding="utf-8"),
]
for field in sorted(PLAN_EVALUATION_FIELDS):
    if any(field not in contract for contract in plan_contracts):
        fail(f"PlanEvaluation field is inconsistent: {field}")

trip_context_contracts = [
    (ROOT / "contracts/care-engine-contracts.yaml").read_text(encoding="utf-8"),
    (
        ROOT / "contracts/generated/java/com/neepo/contracts/TripContext.java"
    ).read_text(encoding="utf-8"),
    (
        ROOT / "contracts/generated/typescript/neepo-contracts.ts"
    ).read_text(encoding="utf-8"),
]
for field in sorted(TRIP_CONTEXT_FIELDS):
    if any(field not in contract for contract in trip_context_contracts):
        fail(f"TripContext field is inconsistent: {field}")

migrations = sorted(
    path.name
    for path in (ROOT / "backend/src/main/resources/db/migration").glob("V*.sql")
)
expected_migrations = [
    "V1__create_identity_foundation.sql",
    "V2__create_operational_domain.sql",
    "V3__create_context_and_decision_domain.sql",
    "V4__seed_repeatable_demo_data.sql",
    "V5__add_user_credentials.sql",
    "V6__correct_demo_user_credentials.sql",
]
if migrations != expected_migrations:
    fail(f"unexpected Flyway migration order: {migrations}")

seed_uuids = set(
    re.findall(
        r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        seed_text,
    )
)
if len(seed_uuids) < 20:
    fail("repeatable demo seed contains too few stable identifiers")
for value in seed_uuids:
    uuid.UUID(value)

for forbidden in ("jdbc:mysql", "mysql-connector", "ddl-auto: update", "newpassword"):
    for path in source_files():
        if path == Path(__file__).resolve():
            continue
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if forbidden in content:
            fail(f"forbidden legacy value '{forbidden}' appears in {path.relative_to(ROOT)}")

print("NEEPO-CARE pre-development foundation validation passed.")
