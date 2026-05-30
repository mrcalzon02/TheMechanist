@echo off
setlocal

if not exist logs mkdir logs

set LOG=logs\compile-sweep-latest.log

echo The Mechanist compile sweep > "%LOG%"
echo Started: %DATE% %TIME% >> "%LOG%"
echo Command: mvn -DskipTests compile >> "%LOG%"
echo. >> "%LOG%"

mvn -DskipTests compile >> "%LOG%" 2>&1
set EXIT_CODE=%ERRORLEVEL%

echo. >> "%LOG%"
echo Finished: %DATE% %TIME% >> "%LOG%"
echo Exit code: %EXIT_CODE% >> "%LOG%"

if not "%EXIT_CODE%"=="0" (
    echo Compile sweep failed. See %LOG%.
    exit /b %EXIT_CODE%
)

echo Compile sweep passed. See %LOG%.
exit /b 0
