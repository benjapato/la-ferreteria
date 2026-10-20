$ErrorActionPreference = 'Stop'

$launcherDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $launcherDirectory
$runtimeDirectory = Join-Path $root '.runtime'
$pidFile = Join-Path $runtimeDirectory 'la-ferreteria.pid'
$jarName = 'LaFerreteriaReactBackend-0.0.1-SNAPSHOT.jar'

function Show-ApplicationError([string] $message) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show(
        $message,
        'La Ferreteria',
        [System.Windows.MessageBoxButton]::OK,
        [System.Windows.MessageBoxImage]::Error
    ) | Out-Null
}

function Get-VerifiedProcess([int] $processId) {
    $process = Get-CimInstance `
        -ClassName Win32_Process `
        -Filter "ProcessId = $processId" `
        -ErrorAction SilentlyContinue

    if ($process -and $process.CommandLine -like "*$jarName*") {
        return $process
    }

    return $null
}

function Get-DescendantIds([int] $parentId) {
    $pending = [System.Collections.Generic.Queue[int]]::new()
    $result = [System.Collections.Generic.List[int]]::new()
    $pending.Enqueue($parentId)

    while ($pending.Count -gt 0) {
        $current = $pending.Dequeue()
        $children = Get-CimInstance `
            -ClassName Win32_Process `
            -Filter "ParentProcessId = $current" `
            -ErrorAction SilentlyContinue

        foreach ($child in $children) {
            $result.Add([int] $child.ProcessId)
            $pending.Enqueue([int] $child.ProcessId)
        }
    }

    return $result
}

try {
    $applicationProcess = $null

    if (Test-Path -LiteralPath $pidFile) {
        $storedPid = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue
        if ($storedPid -match '^\d+$') {
            $applicationProcess = Get-VerifiedProcess -processId ([int] $storedPid)
        }
    }

    if (-not $applicationProcess) {
        $listener = Get-NetTCPConnection `
            -LocalPort 9092 `
            -State Listen `
            -ErrorAction SilentlyContinue `
            | Select-Object -First 1

        if ($listener) {
            $applicationProcess = Get-VerifiedProcess -processId $listener.OwningProcess
        }
    }

    if (-not $applicationProcess) {
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
        exit 0
    }

    $descendants = @(Get-DescendantIds -parentId $applicationProcess.ProcessId)
    [array]::Reverse($descendants)
    foreach ($processId in $descendants) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
    Stop-Process -Id $applicationProcess.ProcessId -Force -ErrorAction SilentlyContinue

    $deadline = (Get-Date).AddSeconds(10)
    while ((Get-Date) -lt $deadline) {
        if (-not (Get-Process -Id $applicationProcess.ProcessId -ErrorAction SilentlyContinue)) {
            break
        }
        Start-Sleep -Milliseconds 250
    }

    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
} catch {
    Show-ApplicationError "No se pudo cerrar La Ferreteria.`n`n$($_.Exception.Message)"
    exit 1
}
