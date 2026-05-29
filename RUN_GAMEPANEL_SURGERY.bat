@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

if not exist "logs\gamepanel_surgery" mkdir "logs\gamepanel_surgery" >nul 2>nul
for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set STAMP=%%I
set LOGFILE=logs\gamepanel_surgery\gamepanel_surgery_%STAMP%.log

call :RUN_SURGERY > "%LOGFILE%" 2>&1
set RESULT=%ERRORLEVEL%

type "%LOGFILE%"
echo.
echo ============================================================
echo Log file: %CD%\%LOGFILE%
echo ============================================================
if not "%RESULT%"=="0" (
    echo [FAIL] GamePanel surgery failed. Send the log file above.
    pause
    exit /b %RESULT%
)
echo [DONE] GamePanel surgery runner completed successfully.
pause
exit /b 0

:RUN_SURGERY
setlocal EnableExtensions EnableDelayedExpansion

echo ============================================================
echo The Mechanist - GamePanel Surgery Runner
echo ============================================================
echo Repo: %CD%
echo Log: %CD%\%LOGFILE%
echo.

where python >nul 2>nul
if errorlevel 1 (
    echo [FAIL] Python was not found on PATH.
    echo Install Python or fix PATH, then rerun this file.
    exit /b 1
)

where git >nul 2>nul
if errorlevel 1 (
    echo [FAIL] Git was not found on PATH.
    echo Install Git or fix PATH, then rerun this file.
    exit /b 1
)

if not exist "ROOT_tools\gamepanel_surgery.py" (
    echo [FAIL] Missing ROOT_tools\gamepanel_surgery.py.
    echo Run git pull from the repository root, then rerun this file.
    exit /b 1
)

if not exist "ROOT_tools\repair_gamepanel_identifier_fractures.py" (
    echo [WARN] Missing ROOT_tools\repair_gamepanel_identifier_fractures.py.
    echo Identifier repair pass will be skipped.
)

echo [1/8] Syncing main with origin using fast-forward only...
git pull --ff-only
if errorlevel 1 (
    echo.
    echo [FAIL] git pull --ff-only failed.
    echo Resolve local divergence/conflicts first, then rerun this file.
    exit /b 1
)

echo.
echo [2/8] Running exactly one auto-detected GamePanel surgery stage without compiling yet...
echo       Compile happens after the repair pass so fixable identifier fractures do not spam the screen.
python .\ROOT_tools\gamepanel_surgery.py --stage next --no-compile
if errorlevel 1 (
    echo.
    echo [FAIL] GamePanel surgery stage failed before compile.
    echo No automatic commit or push was attempted.
    exit /b 1
)

echo.
echo [3/8] Repairing known Java identifier fractures before compile...
if exist "ROOT_tools\repair_gamepanel_identifier_fractures.py" (
    python .\ROOT_tools\repair_gamepanel_identifier_fractures.py
    if errorlevel 1 (
        echo.
        echo [FAIL] Identifier fracture repair failed.
        exit /b 1
    )
) else (
    echo [SKIP] repair_gamepanel_identifier_fractures.py not present.
)

echo.
echo [4/8] Refreshing GamePanel catalog after surgery and repairs...
python .\ROOT_tools\gamepanel_surgery.py --stage catalog --no-compile
if errorlevel 1 (
    echo.
    echo [FAIL] Catalog refresh failed.
    exit /b 1
)

echo.
echo [5/8] Compiling project. Full compiler output is captured in this log...
python .\ROOT_tools\gamepanel_surgery.py --stage compile --no-catalog
if errorlevel 1 (
    echo.
    echo [FAIL] javac compile failed.
    echo The full compile output is in: %CD%\%LOGFILE%
    echo No automatic commit or push was attempted.
    exit /b 1
)

echo.
echo [6/8] Removing local backup artifacts from commit scope...
if exist "src\mechanist\GamePanel.java.bak" (
    del /q "src\mechanist\GamePanel.java.bak" >nul 2>nul
)

echo.
echo [7/8] Checking repository changes...
git status --short
set HAS_CHANGES=
for /f %%A in ('git status --porcelain') do set HAS_CHANGES=1
if not defined HAS_CHANGES (
    echo.
    echo [OK] No local source changes to commit.
    echo Pushing any already-created local commits...
    git push
    if errorlevel 1 (
        echo [FAIL] git push failed.
        exit /b 1
    )
    echo [DONE] Nothing new to commit.
    exit /b 0
)

echo.
echo [8/8] Staging, committing, and pushing surgery outputs...
git add -A ROOT_tools docs src RUN_GAMEPANEL_SURGERY.bat
git reset -- src\mechanist\GamePanel.java.bak >nul 2>nul

git commit -m "Architecture: run automated GamePanel surgery stage"
if errorlevel 1 (
    echo.
    echo [WARN] git commit did not create a commit. This may mean there were no staged changes.
) else (
    git push
    if errorlevel 1 (
        echo.
        echo [FAIL] git push failed after commit.
        exit /b 1
    )
)

echo.
echo ============================================================
echo DONE - One GamePanel surgery stage, repair pass, catalog refresh, compile, commit, push.
echo Rerun this batch file when you want the next automated stage.
echo ============================================================
exit /b 0
