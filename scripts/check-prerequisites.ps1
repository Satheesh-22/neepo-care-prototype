$ErrorActionPreference = "Stop"
$failures = 0

function Test-RequiredCommand {
    param([string]$Name)
    if (Get-Command $Name -ErrorAction SilentlyContinue) {
        Write-Host "FOUND: $Name" -ForegroundColor Green
    } else {
        Write-Host "MISSING: $Name" -ForegroundColor Red
        $script:failures++
    }
}

function Test-MinimumVersion {
    param(
        [string]$Label,
        [string]$VersionText,
        [version]$Minimum
    )

    $match = [regex]::Match($VersionText, '\d+(?:\.\d+){0,3}')
    if (-not $match.Success) {
        Write-Host "ERROR: Could not determine the $Label version from '$VersionText'." -ForegroundColor Red
        $script:failures++
        return
    }

    $actual = [version]$match.Value
    if ($actual -lt $Minimum) {
        Write-Host "ERROR: $Label $Minimum or newer is required; found $actual." -ForegroundColor Red
        $script:failures++
    } else {
        Write-Host "$Label version: $actual" -ForegroundColor Green
    }
}

@("git", "java", "node", "npm", "python", "docker") | ForEach-Object { Test-RequiredCommand $_ }

if (Get-Command git -ErrorAction SilentlyContinue) {
    Test-MinimumVersion "Git" (& git --version) ([version]"2.40")
}

if (Get-Command java -ErrorAction SilentlyContinue) {
    $javaVersion = (& cmd.exe /d /c "java -version 2>&1" | Select-Object -First 1).ToString()
    Write-Host $javaVersion
    if ($javaVersion -notmatch '"21') {
        Write-Host "ERROR: Java 21 is required." -ForegroundColor Red
        $failures++
    }
}

if (Get-Command node -ErrorAction SilentlyContinue) {
    $nodeMajor = [int]((& node --version).TrimStart('v').Split('.')[0])
    if ($nodeMajor -lt 22) {
        Write-Host "ERROR: Node.js 22 or newer is required." -ForegroundColor Red
        $failures++
    }
}

if (Get-Command npm -ErrorAction SilentlyContinue) {
    Test-MinimumVersion "npm" (& npm --version) ([version]"10.0")
}

if (Get-Command python -ErrorAction SilentlyContinue) {
    Test-MinimumVersion "Python" (& python --version 2>&1) ([version]"3.11")
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Test-MinimumVersion "Docker" (& docker --version) ([version]"25.0")
    & docker compose version
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Docker Compose v2 is required." -ForegroundColor Red
        $failures++
    }
}

if (Get-Command claude -ErrorAction SilentlyContinue) {
    Write-Host "FOUND: Claude Code" -ForegroundColor Green
} else {
    Write-Host "INFO: Claude Code was not found. Use docs/assistant/FREE_WORKFLOW.md for the file-handoff workflow." -ForegroundColor Yellow
}

if ($failures -gt 0) {
    throw "$failures required prerequisite check(s) failed."
}

Write-Host "All required prerequisite checks passed." -ForegroundColor Green
