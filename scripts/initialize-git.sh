#!/usr/bin/env bash
set -euo pipefail

if [ -d .git ]; then
  echo "A Git repository already exists. No changes made."
  exit 0
fi

if [ -z "$(git config --get user.name || true)" ] \
    || [ -z "$(git config --get user.email || true)" ]; then
  echo "ERROR: Git user.name and user.email must be configured before initialization."
  echo 'Run: git config --global user.name "Your Name"'
  echo 'Run: git config --global user.email "you@example.com"'
  exit 1
fi

git init -b main
git add .
git commit -m "chore: complete NEEPO-CARE pre-development foundation"
git branch develop
git branch feature/backend-platform develop
git branch feature/care-engine develop
git branch feature/frontend-pwa develop

echo "Created main, develop, feature/backend-platform, feature/care-engine and feature/frontend-pwa."
echo "Add a private remote and push each branch before Claude Code development."
