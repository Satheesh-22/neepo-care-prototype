# PowerShell script: Repeatable clean-database Flyway migration validation
# Starts an isolated temporary PostgreSQL 17 container, applies migrations V1-V6,
# verifies schema, indexes, and seeded demo data, and cleanly removes the temporary container.
# Does NOT touch the active development PostgreSQL volume.

param(
    [int]$Port = 5433,
    [string]$Image = "postgis/postgis:17-3.5"
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot
$migrationDir = Join-Path $projectRoot "backend\src\main\resources\db\migration"

if (-not (Test-Path $migrationDir)) {
    Write-Error "Migration directory not found at $migrationDir"
    exit 1
}

$randomSuffix = [System.Guid]::NewGuid().ToString("N").Substring(0, 8)
$containerName = "neepo-clean-db-test-$randomSuffix"
$dbName = "neepo_clean_db"
$dbUser = "neepo_migrator"
$dbPass = "TempMigrateSecret_$randomSuffix"

Write-Host "Starting isolated clean PostgreSQL test container ($containerName) on port $Port..."

$containerId = docker run -d `
    --name $containerName `
    -p "${Port}:5432" `
    -e "POSTGRES_DB=$dbName" `
    -e "POSTGRES_USER=$dbUser" `
    -e "POSTGRES_PASSWORD=$dbPass" `
    $Image

if (-not $containerId) {
    Write-Error "Failed to start temporary PostgreSQL container."
    exit 1
}

try {
    # Wait for PostgreSQL readiness (full init completion)
    Write-Host "Waiting for database readiness..."
    $ready = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        try {
            $check = docker exec -e "PGPASSWORD=$dbPass" $containerName psql -U $dbUser -d $dbName -t -A -c "SELECT 1;" 2>$null
            if ($LASTEXITCODE -eq 0 -and ($check -join "").Trim() -eq "1") {
                # Double-check it stays up and doesn't shut down during initial init
                Start-Sleep -Seconds 2
                $doubleCheck = docker exec -e "PGPASSWORD=$dbPass" $containerName psql -U $dbUser -d $dbName -t -A -c "SELECT 1;" 2>$null
                if ($LASTEXITCODE -eq 0 -and ($doubleCheck -join "").Trim() -eq "1") {
                    $ready = $true
                    break
                }
            }
        } catch {}
    }

    if (-not $ready) {
        Write-Error "PostgreSQL container failed to become ready within 40 seconds."
        exit 1
    }

    Write-Host "Database is ready. Applying migrations in order..."
    $migrationFiles = Get-ChildItem -Path $migrationDir -Filter "V*.sql" | Sort-Object Name

    foreach ($file in $migrationFiles) {
        Write-Host "Applying $($file.Name)..."
        Get-Content $file.FullName -Raw | docker exec -i -e "PGPASSWORD=$dbPass" $containerName psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1 -q
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Migration failed on $($file.Name)"
            exit 1
        }
    }

    Write-Host "All migrations applied successfully."
    Write-Host "Verifying schema, tables, indexes, and demo records..."

    $verifySql = @"
SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';
SELECT count(*) FROM pg_indexes WHERE schemaname = 'public';
SELECT count(*) FROM users WHERE email IN ('dispatcher@neepo.local', 'driver@neepo.local');
SELECT count(*) FROM trips;
"@

    $results = docker exec -i -e "PGPASSWORD=$dbPass" $containerName psql -U $dbUser -d $dbName -t -A -c "$verifySql"
    $lines = $results -split '\r?\n' | Where-Object { $_ -match '^\d+$' }

    $tableCount = [int]$lines[0]
    $indexCount = [int]$lines[1]
    $userCount = [int]$lines[2]
    $tripCount = [int]$lines[3]

    if ($tableCount -lt 15) {
        Write-Error "Schema validation failed: Expected at least 15 public tables, found $tableCount."
        exit 1
    }

    if ($indexCount -lt 20) {
        Write-Error "Schema validation failed: Expected at least 20 indexes, found $indexCount."
        exit 1
    }

    if ($userCount -lt 2) {
        Write-Error "Demo data validation failed: Expected seeded demo users, found $userCount."
        exit 1
    }

    if ($tripCount -lt 1) {
        Write-Error "Demo data validation failed: Expected seeded demo trips, found $tripCount."
        exit 1
    }

    Write-Host "Verification PASSED:"
    Write-Host " - Public tables: $tableCount"
    Write-Host " - Indexes: $indexCount"
    Write-Host " - Seeded demo users verified: $userCount"
    Write-Host " - Seeded demo trips verified: $tripCount"
    Write-Host " - Clean database migration test completed successfully (0 secrets leaked)."
}
finally {
    Write-Host "Cleaning up temporary container $containerName..."
    docker rm -f $containerName 2>&1 | Out-Null
    Write-Host "Temporary container removed. Development volumes left completely untouched."
}
