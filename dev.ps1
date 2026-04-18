param(
    [ValidateSet('Start', 'Stop', 'Restart')]
    [string]$Action = 'Start',
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173
)

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $rootDir 'jxc'
$frontendDir = Join-Path $rootDir 'jxc-w'
$stateDir = Join-Path $rootDir '.dev'
$pidDir = Join-Path $stateDir 'pids'

New-Item -ItemType Directory -Force -Path $pidDir | Out-Null

function Get-PortPids {
    param(
        [int]$Port
    )

    try {
        return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
            Select-Object -ExpandProperty OwningProcess -Unique)
    } catch {
        return @(netstat -ano |
            Select-String ":$Port\s+.*LISTENING\s+(\d+)$" |
            ForEach-Object {
                $parts = ($_ -replace '\s+', ' ').Trim().Split(' ')
                $parts[-1]
            } |
            Select-Object -Unique)
    }
}

function Stop-ProcessTree {
    param(
        [int]$ProcessId
    )

    if ($ProcessId -le 0) {
        return
    }

    if ($ProcessId -eq $PID) {
        return
    }

    try {
        & taskkill /PID $ProcessId /T /F | Out-Null
    } catch {
        try {
            Stop-Process -Id $ProcessId -Force -ErrorAction Stop
        } catch {
            Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
        }
    }
}

function Stop-Port {
    param(
        [int]$Port,
        [string]$Name
    )

    $pidFile = Join-Path $pidDir "$Name.pid"
    if (Test-Path -LiteralPath $pidFile) {
        $pidText = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($pidText -match '^\d+$') {
            Stop-ProcessTree -ProcessId ([int]$pidText)
        }
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    }

    foreach ($pidText in (Get-PortPids -Port $Port)) {
        if ($pidText -match '^\d+$') {
            Stop-ProcessTree -ProcessId ([int]$pidText)
        }
    }
}

function Start-ServiceProcess {
    param(
        [string]$Name,
        [string]$WorkingDir,
        [string]$CommandLine
    )

    $process = Start-Process -FilePath 'cmd.exe' `
        -ArgumentList @('/k', "title $Name && cd /d `"$WorkingDir`" && $CommandLine") `
        -PassThru

    Set-Content -LiteralPath (Join-Path $pidDir "$Name.pid") -Value $process.Id
    Write-Host "$Name started. PID=$($process.Id)"
}

function Start-Backend {
    Stop-Port -Port $BackendPort -Name 'backend'
    Start-ServiceProcess `
        -Name 'backend' `
        -WorkingDir $backendDir `
        -CommandLine "mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=$BackendPort"
}

function Start-Frontend {
    Stop-Port -Port $FrontendPort -Name 'frontend'
    Start-ServiceProcess `
        -Name 'frontend' `
        -WorkingDir $frontendDir `
        -CommandLine "npm.cmd run dev -- --host 0.0.0.0 --port $FrontendPort"
}

function Stop-All {
    Stop-Port -Port $FrontendPort -Name 'frontend'
    Stop-Port -Port $BackendPort -Name 'backend'
    Write-Host 'All dev processes stopped.'
}

switch ($Action) {
    'Start' {
        Start-Backend
        Start-Frontend
        Write-Host "Frontend: http://localhost:$FrontendPort"
        Write-Host "Backend:  http://localhost:$BackendPort"
    }
    'Stop' {
        Stop-All
    }
    'Restart' {
        Stop-All
        Start-Sleep -Seconds 2
        Start-Backend
        Start-Frontend
        Write-Host "Frontend: http://localhost:$FrontendPort"
        Write-Host "Backend:  http://localhost:$BackendPort"
    }
}
