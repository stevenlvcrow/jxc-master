param(
    [string[]]$MavenArgs
)

$scriptPath = Join-Path $PSScriptRoot "scripts\start-backend.ps1"

if (-not (Test-Path $scriptPath)) {
    Write-Error "Cannot find script: $scriptPath"
    exit 1
}

if ($MavenArgs -and $MavenArgs.Count -gt 0) {
    & powershell -ExecutionPolicy Bypass -File $scriptPath -MavenArgs $MavenArgs
} else {
    & powershell -ExecutionPolicy Bypass -File $scriptPath
}
