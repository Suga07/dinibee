$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$target = Join-Path $root ".env"
if (Test-Path $target) { throw ".env already exists; keep your existing secrets." }
function New-Secret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$values = @{
    DB_PASSWORD = (New-Secret)
    JWT_SECRET = (New-Secret)
    AUTH_TOKEN_PEPPER = (New-Secret)
    MAIL_ENCRYPTION_KEY = (New-Secret)
    SETTLEMENT_ENCRYPTION_KEY = (New-Secret)
}
$lines = Get-Content (Join-Path $root ".env.example") | ForEach-Object {
    $key = ($_ -split "=", 2)[0]
    if ($values.ContainsKey($key)) { "$key=$($values[$key])" } else { $_ }
}
[System.IO.File]::WriteAllLines($target, [string[]]$lines, (New-Object System.Text.UTF8Encoding $false))
Write-Output "Created .env with unique secrets. Keep this file private."
