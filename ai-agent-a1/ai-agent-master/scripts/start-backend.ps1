Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$EnvFile = Join-Path $Root ".env"
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $name, $value = $line.Split("=", 2)
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "java was not found. Install JDK 21 and reopen PowerShell."
    exit 1
}

$javaVersionOutput = cmd.exe /c "java -version 2>&1"
$javaVersion = $javaVersionOutput | Select-Object -First 1
if ($javaVersion -notmatch '"21\.') {
    Write-Host "This backend requires Java 21. Current java -version output:"
    Write-Host $javaVersion
    Write-Host ""
    Write-Host "Install JDK 21 or set JAVA_HOME/PATH to a Java 21 installation, then run this script again."
    exit 1
}

if (-not $env:DASHSCOPE_API_KEY -and -not $env:api_key) {
    Write-Host "Missing DashScope key. Set one of these environment variables first, for example:"
    Write-Host '$env:DASHSCOPE_API_KEY="your-dashscope-api-key"'
    Write-Host '$env:api_key="your-dashscope-api-key"'
    exit 1
}

if (-not $env:SPRING_APPLICATION_JSON) {
    $env:SPRING_APPLICATION_JSON = '{"app":{"vectorstore":{"pgvector":{"enabled":false}}},"spring":{"autoconfigure":{"exclude":["org.springframework.ai.autoconfigure.mcp.client.McpClientAutoConfiguration","org.springframework.ai.autoconfigure.mcp.client.StdioTransportAutoConfiguration"]}}}'
}

Write-Host "Backend database persistence: $($env:APP_JDBC_PERSISTENCE_ENABLED)"
if ($env:APP_JDBC_PERSISTENCE_ENABLED -eq "true") {
    Write-Host "Database URL: $($env:DB_URL)"
}

.\mvnw.cmd spring-boot:run
