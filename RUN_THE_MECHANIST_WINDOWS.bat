@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title The Mechanist Launcher

if not exist "%~dp0RUN_THE_MECHANIST_WINDOWS.ps1" (
  echo ERROR: RUN_THE_MECHANIST_WINDOWS.ps1 is missing from this folder.
  echo The game zip may not have extracted correctly.
  echo.
  pause
  exit /b 2
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0RUN_THE_MECHANIST_WINDOWS.ps1" %*
set "LAUNCH_EXIT=%ERRORLEVEL%"

if not "%LAUNCH_EXIT%"=="0" (
  echo.
  echo The Mechanist launcher exited with code %LAUNCH_EXIT%.
  echo The window is staying open so the error is visible.
  echo.
  pause
)
exit /b %LAUNCH_EXIT%
