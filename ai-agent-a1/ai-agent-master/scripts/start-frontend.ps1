Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Frontend = Join-Path $Root "ai-agent-frontend"

Set-Location $Frontend
if (-not (Test-Path "node_modules")) {
    $npm = Get-Command npm.cmd,npm -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $npm) {
        Write-Host "npm was not found. Install Node.js 18+ and reopen PowerShell."
        exit 1
    }
    & $npm.Source install
}

$npm = Get-Command npm.cmd,npm -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $npm) {
    Write-Host "npm was not found. Install Node.js 18+ and reopen PowerShell."
    exit 1
}

& $npm.Source run dev -- --host 127.0.0.1 --port 5173
