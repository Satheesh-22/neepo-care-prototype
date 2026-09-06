#!/usr/bin/env bash
set -euo pipefail

chmod +x backend/mvnw scripts/*.sh
./scripts/check-prerequisites.sh
python3 scripts/validate-foundation.py

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example. Replace every placeholder before starting Docker."
else
  echo ".env already exists and was preserved."
fi

echo "Foundation checks complete. Review docs/claude/START_HERE.md next."
