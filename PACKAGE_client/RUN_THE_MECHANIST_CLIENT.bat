@echo off
setlocal
cd /d "%~dp0"
java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist
set MECH_EXIT=%ERRORLEVEL%
echo.
echo The Mechanist client exited with code %MECH_EXIT%.
pause
exit /b %MECH_EXIT%
