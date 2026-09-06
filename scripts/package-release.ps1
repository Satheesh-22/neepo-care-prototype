# PowerShell script: Package reproducible, secret-free NEEPO-CARE release artifact

param(
    [string]$Version = "1.0.0",
    [string]$OutputDir = "dist\release"
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot

$releaseName = "neepo-care-prototype-v$Version"
$targetDir = Join-Path $projectRoot $OutputDir
$zipPath = Join-Path $targetDir "$releaseName.zip"
$stageDir = Join-Path $targetDir "staging\$releaseName"

Write-Host "Creating release package for NEEPO-CARE v$Version..."

if (Test-Path $stageDir) {
    Remove-Item -Recurse -Force $stageDir
}
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}
New-Item -ItemType Directory -Force -Path $stageDir | Out-Null

$itemsToCopy = @(
    "backend",
    "care-engine",
    "contracts",
    "database",
    "docker",
    "docs",
    "frontend",
    "scripts",
    ".github",
    ".dockerignore",
    ".editorconfig",
    ".gitattributes",
    ".gitignore",
    ".env.example",
    "CLAUDE.md",
    "docker-compose.yml",
    "HANDOFF_STATUS.md",
    "README.md"
)

$excludeDirs = @("node_modules", "target", "dist", ".vite", "coverage", "playwright-report", "test-results", "staging", ".claude", ".git")
$excludeFiles = @(".env", "*.log", "*.tmp")

function Copy-FilteredDirectory($sourceDir, $destinationDir) {
    if (-not (Test-Path $destinationDir)) {
        New-Item -ItemType Directory -Force -Path $destinationDir | Out-Null
    }
    
    Get-ChildItem -Path $sourceDir -Force | ForEach-Object {
        $item = $_
        if ($item.PSIsContainer) {
            if ($excludeDirs -notcontains $item.Name) {
                Copy-FilteredDirectory $item.FullName (Join-Path $destinationDir $item.Name)
            }
        } else {
            $skip = $false
            if ($item.Name -eq ".env" -or $item.Name.EndsWith(".log") -or $item.Name.EndsWith(".tmp")) {
                $skip = $true
            }
            if (-not $skip) {
                Copy-Item -Path $item.FullName -Destination (Join-Path $destinationDir $item.Name) -Force
            }
        }
    }
}

foreach ($item in $itemsToCopy) {
    $src = Join-Path $projectRoot $item
    if (Test-Path $src) {
        $dest = Join-Path $stageDir $item
        if (Test-Path $src -PathType Container) {
            Copy-FilteredDirectory $src $dest
        } else {
            if ($item -ne ".env" -and -not $item.EndsWith(".log")) {
                Copy-Item -Path $src -Destination $dest -Force
            }
        }
    }
}

# Clean unwanted build caches, node_modules, and target directories from staging
$purgePatterns = @(
    "node_modules",
    "target",
    "dist",
    ".vite",
    "coverage",
    "playwright-report",
    "test-results",
    "*.log",
    ".env"
)

foreach ($pattern in $purgePatterns) {
    Get-ChildItem -Path $stageDir -Recurse -Filter $pattern -Force -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -Recurse -Force $_.FullName
    }
    Get-ChildItem -Path $stageDir -Recurse -Directory -Filter $pattern -Force -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -Recurse -Force $_.FullName
    }
}

# Explicitly ensure NO .env file exists in the staged release
$forbiddenEnv = Join-Path $stageDir ".env"
if (Test-Path $forbiddenEnv) {
    Remove-Item -Force $forbiddenEnv
}

Write-Host "Compressing release archive to $zipPath..."
Compress-Archive -Path "$stageDir\*" -DestinationPath $zipPath -CompressionLevel Optimal

# Verify archive contents for secrets and excluded directories
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
$violations = @()

foreach ($entry in $zip.Entries) {
    $name = $entry.FullName
    if ($name -match '(\.env$|node_modules|target/|dist/|\.log$)') {
        $violations += $name
    }
}
$zip.Dispose()

# Remove temporary staging directory
Remove-Item -Recurse -Force (Join-Path $targetDir "staging")

if ($violations.Count -gt 0) {
    Remove-Item -Force $zipPath
    Write-Error "Release packaging failed: Excluded file patterns detected in archive: $($violations -join ', ')"
    exit 1
}

$hash = (Get-FileHash -Path $zipPath -Algorithm SHA256).Hash
$fileSize = (Get-Item $zipPath).Length

Write-Host "Release packaging SUCCEEDED:"
Write-Host " - Archive: $zipPath"
Write-Host " - Size: $([math]::Round($fileSize / 1MB, 2)) MB"
Write-Host " - SHA-256: $hash"
Write-Host " - Secrets & caches verified absent."
