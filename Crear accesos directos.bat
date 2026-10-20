@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0.aplicacion\crear-accesos-directos.ps1"

if errorlevel 1 (
    echo.
    echo No se pudieron crear los accesos directos.
    pause
    exit /b 1
)

echo.
echo Los accesos directos se crearon en el escritorio.
pause
