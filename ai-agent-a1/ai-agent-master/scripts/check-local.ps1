Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Frontend = Join-Path $Root "ai-agent-frontend"

function Write-Section {
    param([string]$Text)
    Write-Host ""
    Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Text)
    Write-Host "[OK] $Text" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Text)
    Write-Host "[WARN] $Text" -ForegroundColor Yellow
}

function Fail {
    param([string]$Text)
    Write-Host "[FAIL] $Text" -ForegroundColor Red
    exit 1
}

function Load-EnvFile {
    $EnvFile = Join-Path $Root ".env"
    if (-not (Test-Path $EnvFile)) {
        Write-Warn ".env not found. Build can continue, but AI calls need DASHSCOPE_API_KEY when running."
        return
    }

    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $name, $value = $line.Split("=", 2)
        [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
    }
    Write-Ok ".env loaded"
}

function Get-CommandSource {
    param([string[]]$Names)
    $command = Get-Command $Names -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $command) {
        return $null
    }
    return $command.Source
}

Set-Location $Root

Write-Section "Git branch"
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne "Li") {
    Fail "Current branch is '$branch'. Please switch to Li before local acceptance checks."
}
Write-Ok "Current branch: Li"
$status = git status --short
if ($status) {
    Write-Warn "Working tree has uncommitted changes. This is okay during development; commit after verification."
} else {
    Write-Ok "Working tree is clean"
}

Write-Section "Environment"
Load-EnvFile

$java = Get-CommandSource @("java.exe", "java")
if (-not $java) {
    Fail "java was not found. Install JDK 21 and reopen PowerShell."
}
$javaVersionOutput = cmd.exe /c "java -version 2>&1"
$javaVersion = $javaVersionOutput | Select-Object -First 1
if ($javaVersion -notmatch '"21\.') {
    Fail "Java 21 is required. Current java -version: $javaVersion"
}
Write-Ok "Java: $javaVersion"

$node = Get-CommandSource @("node.exe", "node")
if (-not $node) {
    Fail "node was not found. Install Node.js 18+ and reopen PowerShell."
}
$nodeVersion = & $node --version
$nodeMajor = [int]($nodeVersion.TrimStart("v").Split(".")[0])
if ($nodeMajor -lt 18) {
    Fail "Node.js 18+ is required. Current version: $nodeVersion"
}
Write-Ok "Node.js: $nodeVersion"

$npm = Get-CommandSource @("npm.cmd", "npm")
if (-not $npm) {
    Fail "npm was not found. Reinstall Node.js with npm enabled."
}
$npmVersion = & $npm --version
Write-Ok "npm: $npmVersion"

if ($env:DASHSCOPE_API_KEY -and $env:DASHSCOPE_API_KEY -ne "your-dashscope-api-key") {
    Write-Ok "DASHSCOPE_API_KEY is configured for runtime AI tests"
} elseif ($env:api_key) {
    Write-Ok "api_key is configured for runtime AI tests"
} else {
    Write-Warn "DashScope key is not configured. Build can pass, but AI question answering will not work at runtime."
}

Write-Section "Optional PostgreSQL"
if ($env:APP_JDBC_PERSISTENCE_ENABLED -eq "true") {
    $psql = Get-CommandSource @("psql.exe", "psql")
    if (-not $psql) {
        $defaultPsql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
        if (Test-Path $defaultPsql) {
            $psql = $defaultPsql
        }
    }

    if (-not $psql) {
        Write-Warn "APP_JDBC_PERSISTENCE_ENABLED=true, but psql was not found in PATH or PostgreSQL 18 default path."
    } else {
        Write-Ok "psql found: $psql"
    }

    if ($env:DB_URL) {
        Write-Ok "DB_URL: $($env:DB_URL)"
    } else {
        Write-Warn "DB_URL is empty; backend will use application.yml default."
    }

    $jdbcUrl = if ($env:DB_URL) { $env:DB_URL } else { "jdbc:postgresql://localhost:5432/ai_agent" }
    $dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "postgres" }
    if ($jdbcUrl -match "^jdbc:postgresql://([^:/?#]+)(?::(\d+))?/([^?]+)") {
        $dbHost = $Matches[1]
        $dbPort = if ($Matches[2]) { $Matches[2] } else { "5432" }
        $dbName = $Matches[3]

        if ($env:DB_PASSWORD) {
            $env:PGPASSWORD = $env:DB_PASSWORD
            & $psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -v ON_ERROR_STOP=1 -c "select 1;" | Out-Null
            Write-Ok "PostgreSQL connection passed: ${dbHost}:${dbPort}/$dbName"
        } else {
            Write-Warn "DB_PASSWORD is empty. Skipped PostgreSQL login check to avoid an interactive password prompt."
        }
    } else {
        Write-Warn "DB_URL format is not recognized. Skipped PostgreSQL login check."
    }
} else {
    Write-Ok "JDBC persistence is disabled for this check. Set APP_JDBC_PERSISTENCE_ENABLED=true in .env to check database runtime config."
}

Write-Section "Backend build"
& (Join-Path $Root "mvnw.cmd") -DskipTests package
Write-Ok "Backend package passed"

Write-Section "Frontend build"
Set-Location $Frontend
if (-not (Test-Path "node_modules")) {
    Write-Warn "node_modules not found. Running npm install first."
    & $npm install
}
& $npm run build
Write-Ok "Frontend build passed"

Write-Section "Result"
Write-Ok "Local acceptance check passed. You can start backend/frontend for manual feature validation."
