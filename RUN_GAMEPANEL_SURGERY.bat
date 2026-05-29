@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

echo ============================================================
echo The Mechanist - GamePanel Surgery Runner
echo ============================================================
echo Repo: %CD%
echo.

where python >nul 2>nul
if errorlevel 1 (
    echo [FAIL] Python was not found on PATH.
    echo Install Python or fix PATH, then rerun this file.
    pause
    exit /b 1
)

where git >nul 2>nul
if errorlevel 1 (
    echo [FAIL] Git was not found on PATH.
    echo Install Git or fix PATH, then rerun this file.
    pause
    exit /b 1
)

if not exist "ROOT_tools\gamepanel_surgery.py" (
    echo [FAIL] Missing ROOT_tools\gamepanel_surgery.py.
    echo Run git pull from the repository root, then rerun this file.
    pause
    exit /b 1
)

echo [1/6] Syncing main with origin using fast-forward only...
git pull --ff-only
if errorlevel 1 (
    echo.
    echo [FAIL] git pull --ff-only failed.
    echo Resolve local divergence/conflicts first, then rerun this file.
    pause
    exit /b 1
)

echo.
echo [2/6] Running exactly one auto-detected GamePanel surgery stage...
echo       This is intentionally single-stage to prevent unattended loops.
python .\ROOT_tools\gamepanel_surgery.py --stage next
if errorlevel 1 (
    echo.
    echo [FAIL] GamePanel surgery or compile failed.
    echo No automatic commit or push was attempted.
    echo Fix the shown error, then rerun this file.
    pause
    exit /b 1
)

echo.
echo [3/6] Removing local backup artifacts from commit scope...
if exist "src\mechanist\GamePanel.java.bak" (
    del /q "src\mechanist\GamePanel.java.bak" >nul 2>nul
)

echo.
echo [4/6] Checking repository changes...
git status --short
set HAS_CHANGES=
for /f %%A in ('git status --porcelain') do set HAS_CHANGES=1
if not defined HAS_CHANGES (
    echo.
    echo [OK] No local source changes to commit.
    echo [5/6] Pushing any already-created local commits...
    git push
    if errorlevel 1 (
        echo [FAIL] git push failed.
        pause
        exit /b 1
    )
    echo.
    echo [DONE] GamePanel surgery runner completed. Nothing new to commit.
    pause
    exit /b 0
)

echo.
echo [5/6] Staging surgery outputs...
git add -A ROOT_tools docs src RUN_GAMEPANEL_SURGERY.bat
git reset -- src\mechanist\GamePanel.java.bak >nul 2>nul

echo.
echo [6/6] Committing and pushing...
git commit -m "Architecture: run automated GamePanel surgery stage"
if errorlevel 1 (
    echo.
    echo [WARN] git commit did not create a commit. This may mean there were no staged changes.
) else (
    git push
    if errorlevel 1 (
        echo.
        echo [FAIL] git push failed after commit.
        pause
        exit /b 1
    )
)

echo.
echo ============================================================
echo DONE - One GamePanel surgery stage, catalog refresh, compile, commit, push.
echo Rerun this batch file when you want the next automated stage.
echo ============================================================
pause
exit /b 0
