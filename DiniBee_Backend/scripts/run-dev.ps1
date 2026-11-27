$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Get-Content (Join-Path $root ".env") | ForEach-Object {
    if ($_ -match "^([A-Z][A-Z0-9_]*)=(.*)$") {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}
Push-Location $root
try { & mvn spring-boot:run; exit $LASTEXITCODE } finally { Pop-Location }
