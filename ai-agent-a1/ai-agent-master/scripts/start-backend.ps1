Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
if ($javaVersion -notmatch '"21\.') {
    Write-Host "This backend requires Java 21. Current java -version output:"
    Write-Host $javaVersion
    exit 1
}

if (-not $env:api_key) {
    Write-Host "Missing DashScope key. Set it first, for example:"
    Write-Host '$env:api_key="your-dashscope-api-key"'
    exit 1
}

if (-not $env:SPRING_APPLICATION_JSON) {
    $env:SPRING_APPLICATION_JSON = '{"app":{"vectorstore":{"pgvector":{"enabled":false}}},"spring":{"autoconfigure":{"exclude":["org.springframework.ai.autoconfigure.mcp.client.McpClientAutoConfiguration","org.springframework.ai.autoconfigure.mcp.client.StdioTransportAutoConfiguration"]}}}'
}

.\mvnw.cmd spring-boot:run
