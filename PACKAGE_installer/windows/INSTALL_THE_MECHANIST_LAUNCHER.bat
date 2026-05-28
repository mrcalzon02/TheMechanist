@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo The Mechanist Launcher Installer
echo ============================================================
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0InstallMechanistLauncher.ps1" %*
set EXITCODE=%ERRORLEVEL%
if not "%EXITCODE%"=="0" (
  echo.
  echo Installer failed with exit code %EXITCODE%.
  echo Read the error above.
  pause
)
exit /b %EXITCODE%
