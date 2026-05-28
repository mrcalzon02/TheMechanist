@echo off
setlocal

rem The Mechanist Windows launcher wrapper.
rem Runs the internal PowerShell launcher with a process-scoped execution-policy bypass.
rem This does not change the user's machine-wide PowerShell policy.

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%RUN_THE_MECHANIST_WINDOWS.ps1"

if not exist "%PS_SCRIPT%" (
    echo The Mechanist launcher could not find:
    echo   %PS_SCRIPT%
    echo.
    echo This package is incomplete or was launched from the wrong directory.
    pause
    exit /b 2
)

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%" %*
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo The Mechanist launcher exited with code %EXIT_CODE%.
    echo The window is staying open so the error is visible.
    echo.
    pause
)

exit /b %EXIT_CODE%
