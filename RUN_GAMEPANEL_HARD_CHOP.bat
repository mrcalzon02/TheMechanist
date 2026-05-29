@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

echo ============================================================
echo The Mechanist - GamePanel Hard Chop Workbench Generator
echo ============================================================
echo Repo: %CD%
echo.

where python >nul 2>nul
if errorlevel 1 (
    echo [FAIL] Python was not found on PATH.
    pause
    exit /b 1
)

where git >nul 2>nul
if errorlevel 1 (
    echo [FAIL] Git was not found on PATH.
    pause
    exit /b 1
)

echo [1/5] Pulling latest tooling...
git pull --ff-only
if errorlevel 1 (
    echo [FAIL] git pull failed. Resolve conflicts/divergence and rerun.
    pause
    exit /b 1
)

echo.
echo [2/5] Refreshing GamePanel method catalog...
python .\ROOT_tools\gamepanel_catalog_slicer.py
if errorlevel 1 (
    echo [FAIL] Catalog slicer failed.
    pause
    exit /b 1
)

echo.
echo [3/5] Hard-chopping GamePanel into connector-readable fragments...
python .\ROOT_tools\gamepanel_hard_chop.py
if errorlevel 1 (
    echo [FAIL] Hard chop failed.
    pause
    exit /b 1
)

echo.
echo [4/5] Staging workbench output...
git add ROOT_tools\gamepanel_hard_chop.py RUN_GAMEPANEL_HARD_CHOP.bat docs\gamepanel_catalog docs\gamepanel_hard_chop

echo.
echo [5/5] Committing and pushing hard-chop workbench...
git commit -m "Architecture: generate GamePanel hard chop workbench"
if errorlevel 1 (
    echo [WARN] No commit created. This usually means there were no changes.
) else (
    git push
    if errorlevel 1 (
        echo [FAIL] git push failed.
        pause
        exit /b 1
    )
)

echo.
echo ============================================================
echo DONE - GamePanel hard chop workbench generated and pushed.
echo Read docs\gamepanel_hard_chop\README.md for fragment map.
echo ============================================================
pause
exit /b 0
