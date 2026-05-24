@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo The Mechanist GitHub Launcher
echo ============================================================
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0MechanistLauncher.ps1" %*
set EXITCODE=%ERRORLEVEL%
if not "%EXITCODE%"=="0" (
  echo.
  echo Launcher failed with exit code %EXITCODE%.
  echo Read the error above.
  pause
)
exit /b %EXITCODE%
