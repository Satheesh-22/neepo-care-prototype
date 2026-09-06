$ErrorActionPreference = "Stop"

powershell -ExecutionPolicy Bypass -File .\scripts\check-prerequisites.ps1
python .\scripts\validate-foundation.py

if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
    Write-Host "Created .env from .env.example. Replace every placeholder before starting Docker." -ForegroundColor Yellow
} else {
    Write-Host ".env already exists and was preserved."
}

Write-Host "Foundation checks complete. Review docs/claude/START_HERE.md next." -ForegroundColor Green
