@echo off
setlocal
cd /d "%~dp0\..\.."
echo Building The Mechanist Windows portable app-image, EXE installer, and MSI installer...
echo.
echo This wrapper uses ExecutionPolicy Bypass for this process only so the packaging script can run from a downloaded project folder.
echo If Maven is unavailable, rerun with: powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1 -UseExistingJar
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%CD%\scripts\package\build-windows-installers.ps1"
if errorlevel 1 (
  echo.
  echo Windows packaging failed. Read the diagnostic message above.
  pause
  exit /b 1
)
echo.
echo Windows packaging complete. Outputs are in dist\installers\windows\
pause
