@echo off
setlocal

powershell.exe -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 9092 -State Listen -ErrorAction SilentlyContinue) { exit 1 }"
if errorlevel 1 (
    echo La Ferreteria esta funcionando y no se puede actualizar mientras esta abierta.
    echo Usa el acceso directo Cerrar La Ferreteria y vuelve a intentarlo.
    pause
    exit /b 1
)

cd /d "%~dp0..\backend"

echo Construyendo La Ferreteria...
call gradlew.bat clean bootJar
if errorlevel 1 (
    echo.
    echo No se pudo construir la aplicacion.
    pause
    exit /b 1
)

echo.
echo Aplicacion creada en:
echo %~dp0..\backend\build\libs\LaFerreteriaReactBackend-0.0.1-SNAPSHOT.jar
pause
