$ErrorActionPreference = 'Stop'

$launcherDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $launcherDirectory
$desktop = [Environment]::GetFolderPath('Desktop')
$wscript = Join-Path $env:WINDIR 'System32\wscript.exe'
$applicationIcon = Join-Path $root 'frontend\public\la-ferreteria.ico'
$shell = New-Object -ComObject WScript.Shell

function New-ApplicationShortcut(
    [string] $name,
    [string] $script,
    [string] $description,
    [int] $iconIndex
) {
    $shortcut = $shell.CreateShortcut((Join-Path $desktop "$name.lnk"))
    $shortcut.TargetPath = $wscript
    $shortcut.Arguments = ('"{0}"' -f (Join-Path $launcherDirectory $script))
    $shortcut.WorkingDirectory = $root
    $shortcut.Description = $description
    $shortcut.IconLocation = "$env:WINDIR\System32\shell32.dll,$iconIndex"
    $shortcut.Save()
}

New-ApplicationShortcut `
    -name 'La Ferreteria' `
    -script 'iniciar.vbs' `
    -description 'Abrir La Ferreteria' `
    -iconIndex 220

if (Test-Path -LiteralPath $applicationIcon) {
    $applicationShortcut = $shell.CreateShortcut((Join-Path $desktop 'La Ferreteria.lnk'))
    $applicationShortcut.IconLocation = "$applicationIcon,0"
    $applicationShortcut.Save()
}

Copy-Item `
    -LiteralPath (Join-Path $desktop 'La Ferreteria.lnk') `
    -Destination (Join-Path $root 'La Ferreteria.lnk') `
    -Force

New-ApplicationShortcut `
    -name 'Cerrar La Ferreteria' `
    -script 'cerrar.vbs' `
    -description 'Cerrar La Ferreteria' `
    -iconIndex 131
