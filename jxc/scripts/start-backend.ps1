param(
    [string[]]$MavenArgs
)

$port = 8080
$pids = @()
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent $scriptDir

Set-Location $projectDir

try {
    $pids = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop |
        Select-Object -ExpandProperty OwningProcess -Unique
} catch {
    $pids = netstat -ano |
        Select-String ":$port\s+.*LISTENING\s+(\d+)$" |
        ForEach-Object {
            $parts = ($_ -replace "\s+", " ").Trim().Split(" ")
            $parts[-1]
        } |
        Select-Object -Unique
}

foreach ($pidText in $pids) {
    if ("$pidText" -notmatch "^\d+$") {
        continue
    }
    $targetPid = [int]$pidText
    if ($targetPid -eq $PID) {
        continue
    }
    Write-Host "Stopping process on port ${port}: PID=$targetPid"
    try {
        Stop-Process -Id $targetPid -ErrorAction Stop
    } catch {
        Stop-Process -Id $targetPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Starting backend with Maven..."
if ($MavenArgs -and $MavenArgs.Count -gt 0) {
    & mvn spring-boot:run @MavenArgs
} else {
    & mvn spring-boot:run
}
