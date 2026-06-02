@echo off
setlocal
cd /d "%~dp0"
java -cp "classes;." mechanist.TheMechanist
set MECH_EXIT=%ERRORLEVEL%
echo.
echo The Mechanist client exited with code %MECH_EXIT%.
pause
exit /b %MECH_EXIT%
