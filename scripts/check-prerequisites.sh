#!/usr/bin/env bash
set -euo pipefail

failures=0

check_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "MISSING: $command_name"
    failures=$((failures + 1))
  else
    echo "FOUND: $command_name"
  fi
}

check_minimum_version() {
  local label="$1"
  local actual="$2"
  local required_major="$3"
  local required_minor="$4"
  local actual_major actual_minor

  actual_major=$(printf '%s' "$actual" | sed -E 's/[^0-9]*([0-9]+).*/\1/')
  actual_minor=$(printf '%s' "$actual" | sed -E 's/[^0-9]*[0-9]+\.([0-9]+).*/\1/')

  if ! [[ "$actual_major" =~ ^[0-9]+$ ]]; then
    echo "ERROR: Could not determine the $label version from '$actual'."
    failures=$((failures + 1))
    return
  fi

  if ! [[ "$actual_minor" =~ ^[0-9]+$ ]]; then
    actual_minor=0
  fi

  if [ "$actual_major" -lt "$required_major" ] \
      || { [ "$actual_major" -eq "$required_major" ] && [ "$actual_minor" -lt "$required_minor" ]; }; then
    echo "ERROR: $label $required_major.$required_minor or newer is required; found $actual."
    failures=$((failures + 1))
  else
    echo "$label version: $actual"
  fi
}

for command_name in git java node npm python3 docker; do
  check_command "$command_name"
done

if command -v git >/dev/null 2>&1; then
  check_minimum_version "Git" "$(git --version)" 2 40
fi

if command -v java >/dev/null 2>&1; then
  java_version=$(java -version 2>&1 | head -n 1)
  echo "$java_version"
  if [[ "$java_version" != *'21.'* && "$java_version" != *'"21"'* ]]; then
    echo "ERROR: Java 21 is required."
    failures=$((failures + 1))
  fi
fi

if command -v node >/dev/null 2>&1; then
  node_major=$(node --version | sed 's/^v//' | cut -d. -f1)
  if [ "$node_major" -lt 22 ]; then
    echo "ERROR: Node.js 22 or newer is required."
    failures=$((failures + 1))
  fi
fi

if command -v npm >/dev/null 2>&1; then
  check_minimum_version "npm" "$(npm --version)" 10 0
fi

if command -v python3 >/dev/null 2>&1; then
  check_minimum_version "Python" "$(python3 --version)" 3 11
fi

if command -v docker >/dev/null 2>&1; then
  check_minimum_version "Docker" "$(docker --version)" 25 0
  if ! docker compose version >/dev/null 2>&1; then
    echo "ERROR: Docker Compose v2 is required."
    failures=$((failures + 1))
  else
    docker compose version
  fi
fi

if command -v claude >/dev/null 2>&1; then
  echo "FOUND: Claude Code"
else
  echo "INFO: Claude Code was not found. Use docs/assistant/FREE_WORKFLOW.md for the file-handoff workflow."
fi

if [ "$failures" -ne 0 ]; then
  echo "$failures required prerequisite check(s) failed."
  exit 1
fi

echo "All required prerequisite checks passed."
