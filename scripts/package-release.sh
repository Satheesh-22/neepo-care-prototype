#!/usr/bin/env bash
# Package reproducible, secret-free NEEPO-CARE release artifact

set -euo pipefail

VERSION="${1:-1.0.0}"
OUTPUT_DIR="${2:-dist/release}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RELEASE_NAME="neepo-care-prototype-v${VERSION}"
TARGET_DIR="${PROJECT_ROOT}/${OUTPUT_DIR}"
ZIP_PATH="${TARGET_DIR}/${RELEASE_NAME}.zip"
STAGE_DIR="${TARGET_DIR}/staging/${RELEASE_NAME}"

echo "Creating release package for NEEPO-CARE v${VERSION}..."

rm -rf "${TARGET_DIR}/staging" "${ZIP_PATH}"
mkdir -p "${STAGE_DIR}"

ITEMS=(
    "backend"
    "care-engine"
    "contracts"
    "database"
    "docker"
    "docs"
    "frontend"
    "scripts"
    ".github"
    ".dockerignore"
    ".editorconfig"
    ".gitattributes"
    ".gitignore"
    ".env.example"
    "CLAUDE.md"
    "docker-compose.yml"
    "HANDOFF_STATUS.md"
    "README.md"
)

for item in "${ITEMS[@]}"; do
    if [ -e "${PROJECT_ROOT}/${item}" ]; then
        cp -R "${PROJECT_ROOT}/${item}" "${STAGE_DIR}/"
    fi
done

# Purge build caches and node_modules
find "${STAGE_DIR}" -type d \( -name "node_modules" -o -name "target" -o -name "dist" -o -name ".vite" -o -name "coverage" -o -name "playwright-report" -o -name "test-results" \) -prune -exec rm -rf {} +
find "${STAGE_DIR}" -type f \( -name "*.log" -o -name ".env" \) -delete

echo "Compressing release archive to ${ZIP_PATH}..."
mkdir -p "${TARGET_DIR}"
(cd "${STAGE_DIR}" && zip -r -q "${ZIP_PATH}" .)

rm -rf "${TARGET_DIR}/staging"

# Verify archive integrity
if unzip -l "${ZIP_PATH}" | grep -E '(\.env$|node_modules|target/|dist/|\.log$)'; then
    echo "ERROR: Excluded file patterns detected in release archive" >&2
    rm -f "${ZIP_PATH}"
    exit 1
fi

FILE_SIZE=$(du -h "${ZIP_PATH}" | cut -f1)
SHA256=$(sha256sum "${ZIP_PATH}" | cut -d' ' -f1)

echo "Release packaging SUCCEEDED:"
echo " - Archive: ${ZIP_PATH}"
echo " - Size: ${FILE_SIZE}"
echo " - SHA-256: ${SHA256}"
echo " - Secrets & caches verified absent."
