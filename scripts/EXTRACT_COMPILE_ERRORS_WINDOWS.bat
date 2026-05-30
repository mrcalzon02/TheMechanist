@echo off
setlocal

if not exist logs mkdir logs

set SOURCE_LOG=logs\compile-sweep-latest.log
set ERROR_LOG=logs\compile-errors-latest.log

if not exist "%SOURCE_LOG%" (
    echo Compile sweep log not found: %SOURCE_LOG%
    echo Run scripts\COMPILE_SWEEP_WINDOWS.bat first.
    exit /b 1
)

echo The Mechanist compile error extraction > "%ERROR_LOG%"
echo Source: %SOURCE_LOG% >> "%ERROR_LOG%"
echo Created: %DATE% %TIME% >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

findstr /N /I /C:"[ERROR]" /C:"error:" /C:"cannot find symbol" /C:"symbol:" /C:"location:" /C:"package" "%SOURCE_LOG%" >> "%ERROR_LOG%"
set EXIT_CODE=%ERRORLEVEL%

if "%EXIT_CODE%"=="1" (
    echo No compiler-error markers found. >> "%ERROR_LOG%"
    echo No compiler-error markers found in %SOURCE_LOG%.
    exit /b 0
)

if not "%EXIT_CODE%"=="0" (
    echo Error extraction failed with exit code %EXIT_CODE%.
    exit /b %EXIT_CODE%
)

echo Extracted compile errors to %ERROR_LOG%.
exit /b 0
