$ErrorActionPreference = 'Stop'

$launcherDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $launcherDirectory
$backend = Join-Path $root 'backend'
$jar = Join-Path $backend 'build\libs\LaFerreteriaReactBackend-0.0.1-SNAPSHOT.jar'
$runtimeDirectory = Join-Path $root '.runtime'
$logsDirectory = Join-Path $root 'logs'
$pidFile = Join-Path $runtimeDirectory 'la-ferreteria.pid'
$outputLog = Join-Path $logsDirectory 'aplicacion.log'
$errorLog = Join-Path $logsDirectory 'aplicacion-error.log'
$applicationUrl = 'http://localhost:9092/app/'

function Open-Application {
    $browserCandidates = @(
        (Join-Path ${env:ProgramFiles(x86)} 'Microsoft\Edge\Application\msedge.exe'),
        (Join-Path $env:ProgramFiles 'Microsoft\Edge\Application\msedge.exe'),
        (Join-Path $env:LOCALAPPDATA 'Microsoft\Edge\Application\msedge.exe'),
        (Join-Path $env:ProgramFiles 'Google\Chrome\Application\chrome.exe'),
        (Join-Path ${env:ProgramFiles(x86)} 'Google\Chrome\Application\chrome.exe'),
        (Join-Path $env:LOCALAPPDATA 'Google\Chrome\Application\chrome.exe')
    )

    $browser = $browserCandidates |
        Where-Object { $_ -and (Test-Path -LiteralPath $_) } |
        Select-Object -First 1

    if ($browser) {
        Start-Process `
            -FilePath $browser `
            -ArgumentList @("--app=$applicationUrl", '--start-maximized')
        return
    }

    Start-Process $applicationUrl
}

function Show-ApplicationError([string] $message) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show(
        $message,
        'La Ferreteria',
        [System.Windows.MessageBoxButton]::OK,
        [System.Windows.MessageBoxImage]::Error
    ) | Out-Null
}

function Test-ApplicationReady {
    try {
        $response = Invoke-WebRequest `
            -Uri $applicationUrl `
            -UseBasicParsing `
            -MaximumRedirection 0 `
            -TimeoutSec 2 `
            -ErrorAction Stop

        return $response.StatusCode -eq 200 `
            -and $response.Content -match 'id="root"'
    } catch {
        return $false
    }
}

try {
    if (-not (Test-Path -LiteralPath $jar)) {
        Show-ApplicationError 'No se encontro la aplicacion compilada. Ejecuta .aplicacion\construir-aplicacion.bat antes de iniciarla.'
        exit 1
    }

    if (Test-ApplicationReady) {
        Open-Application
        exit 0
    }

    $listener = Get-NetTCPConnection `
        -LocalPort 9092 `
        -State Listen `
        -ErrorAction SilentlyContinue `
        | Select-Object -First 1

    if ($listener) {
        Show-ApplicationError "El puerto 9092 esta ocupado por otro programa (proceso $($listener.OwningProcess)). Cerralo y vuelve a iniciar La Ferreteria."
        exit 1
    }

    New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
    New-Item -ItemType Directory -Path $logsDirectory -Force | Out-Null

    $java = (Get-Command javaw.exe -ErrorAction Stop).Source
    $process = Start-Process `
        -FilePath $java `
        -ArgumentList @('-jar', ('"{0}"' -f $jar)) `
        -WorkingDirectory $backend `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outputLog `
        -RedirectStandardError $errorLog `
        -PassThru

    Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ascii

    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        if (Test-ApplicationReady) {
            Open-Application
            exit 0
        }

        $process.Refresh()
        if ($process.HasExited) {
            Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
            Show-ApplicationError "La aplicacion no pudo iniciar. Revisa el archivo:`n$errorLog"
            exit 1
        }

        Start-Sleep -Milliseconds 500
    }

    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    Show-ApplicationError "La aplicacion demoro demasiado en iniciar. Revisa los registros en:`n$logsDirectory"
    exit 1
} catch {
    Show-ApplicationError "No se pudo iniciar La Ferreteria.`n`n$($_.Exception.Message)"
    exit 1
}
