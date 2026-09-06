$ErrorActionPreference = "Stop"

if (Test-Path .git) {
    Write-Host "A Git repository already exists. No changes made."
    exit 0
}

$gitUserName = git config --get user.name
$gitUserEmail = git config --get user.email
if ([string]::IsNullOrWhiteSpace($gitUserName) -or [string]::IsNullOrWhiteSpace($gitUserEmail)) {
    throw 'Git user.name and user.email must be configured first. Run git config --global user.name "Your Name" and git config --global user.email "you@example.com".'
}

git init -b main
git add .
git commit -m "chore: complete NEEPO-CARE pre-development foundation"
git branch develop
git branch feature/backend-platform develop
git branch feature/care-engine develop
git branch feature/frontend-pwa develop

Write-Host "Created main, develop, feature/backend-platform, feature/care-engine and feature/frontend-pwa."
Write-Host "Add a private remote and push each branch before Claude Code development."
