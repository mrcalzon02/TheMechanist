@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"
set MAX_FRAGMENT_BYTES=16000
set OUTDIR=docs\gamepanel_sections

echo ============================================================
echo The Mechanist - Connector-Safe GamePanel Section Splitter
echo ============================================================
echo Repo: %CD%
echo Max fragment bytes: %MAX_FRAGMENT_BYTES%
echo Output: %OUTDIR%
echo.

where powershell >nul 2>nul
if errorlevel 1 (
    echo [FAIL] PowerShell was not found on PATH.
    pause
    exit /b 1
)

where git >nul 2>nul
if errorlevel 1 (
    echo [FAIL] Git was not found on PATH.
    pause
    exit /b 1
)

echo [1/4] Pulling latest repository state...
git pull --ff-only
if errorlevel 1 (
    echo [FAIL] git pull --ff-only failed. Resolve divergence/conflicts and rerun.
    pause
    exit /b 1
)

echo.
echo [2/4] Splitting src\mechanist\GamePanel.java into hard connector-safe sections...
set PS_SCRIPT=%TEMP%\section_gamepanel_connector_safe_%RANDOM%%RANDOM%.ps1

> "%PS_SCRIPT%" echo $ErrorActionPreference = 'Stop'
>> "%PS_SCRIPT%" echo $Root = (Get-Location).Path
>> "%PS_SCRIPT%" echo $Source = Join-Path $Root 'src\mechanist\GamePanel.java'
>> "%PS_SCRIPT%" echo $OutDir = Join-Path $Root '%OUTDIR%'
>> "%PS_SCRIPT%" echo $MaxBytes = %MAX_FRAGMENT_BYTES%
>> "%PS_SCRIPT%" echo $Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
>> "%PS_SCRIPT%" echo if (!(Test-Path -LiteralPath $Source)) { throw "Missing source file: $Source" }
>> "%PS_SCRIPT%" echo if (Test-Path -LiteralPath $OutDir) { Remove-Item -LiteralPath $OutDir -Recurse -Force }
>> "%PS_SCRIPT%" echo New-Item -ItemType Directory -Force -Path $OutDir ^| Out-Null
>> "%PS_SCRIPT%" echo $Lines = [System.IO.File]::ReadAllLines($Source, $Utf8NoBom)
>> "%PS_SCRIPT%" echo function ByteCount([string[]]$Parts) { return $Utf8NoBom.GetByteCount(($Parts -join [Environment]::NewLine)) }
>> "%PS_SCRIPT%" echo $Fragments = New-Object System.Collections.Generic.List[object]
>> "%PS_SCRIPT%" echo $Chunk = New-Object System.Collections.Generic.List[string]
>> "%PS_SCRIPT%" echo $Index = 1
>> "%PS_SCRIPT%" echo $StartLine = 1
>> "%PS_SCRIPT%" echo function Write-Fragment([System.Collections.Generic.List[string]]$Chunk, [int]$StartLine, [int]$EndLine) {
>> "%PS_SCRIPT%" echo     if ($Chunk.Count -le 0) { return }
>> "%PS_SCRIPT%" echo     $Name = ('{0:D4}_GamePanel_lines_{1:D5}_{2:D5}.javafrag' -f $script:Index, $StartLine, $EndLine)
>> "%PS_SCRIPT%" echo     $Path = Join-Path $script:OutDir $Name
>> "%PS_SCRIPT%" echo     $Header = @('// Connector-safe GamePanel section fragment.', '// NOT compiled source.', '// Source: src/mechanist/GamePanel.java', ('// Lines: {0}-{1}' -f $StartLine, $EndLine), ('// Max fragment bytes: {0}' -f $script:MaxBytes), '')
>> "%PS_SCRIPT%" echo     $ContentParts = @($Header + [string[]]$Chunk)
>> "%PS_SCRIPT%" echo     $Text = $ContentParts -join [Environment]::NewLine
>> "%PS_SCRIPT%" echo     $Bytes = $script:Utf8NoBom.GetByteCount($Text)
>> "%PS_SCRIPT%" echo     if ($Bytes -gt $script:MaxBytes) { throw "Fragment $Name is $Bytes bytes, over cap $script:MaxBytes. Lower body size or inspect long lines." }
>> "%PS_SCRIPT%" echo     [System.IO.File]::WriteAllText($Path, $Text, $script:Utf8NoBom)
>> "%PS_SCRIPT%" echo     $script:Fragments.Add([pscustomobject]@{ File=$Name; StartLine=$StartLine; EndLine=$EndLine; Bytes=$Bytes; Lines=($EndLine-$StartLine+1) }) ^| Out-Null
>> "%PS_SCRIPT%" echo     $script:Index++
>> "%PS_SCRIPT%" echo }
>> "%PS_SCRIPT%" echo for ($i = 0; $i -lt $Lines.Length; $i++) {
>> "%PS_SCRIPT%" echo     $Line = $Lines[$i]
>> "%PS_SCRIPT%" echo     $Probe = New-Object System.Collections.Generic.List[string]
>> "%PS_SCRIPT%" echo     foreach ($existing in $Chunk) { $Probe.Add($existing) }
>> "%PS_SCRIPT%" echo     $Probe.Add($Line)
>> "%PS_SCRIPT%" echo     $HeaderProbe = @('// Connector-safe GamePanel section fragment.', '// NOT compiled source.', '// Source: src/mechanist/GamePanel.java', ('// Lines: {0}-{1}' -f $StartLine, ($i+1)), ('// Max fragment bytes: {0}' -f $MaxBytes), '')
>> "%PS_SCRIPT%" echo     $ProbeBytes = ByteCount @($HeaderProbe + [string[]]$Probe)
>> "%PS_SCRIPT%" echo     if ($ProbeBytes -gt $MaxBytes -and $Chunk.Count -gt 0) {
>> "%PS_SCRIPT%" echo         Write-Fragment $Chunk $StartLine $i
>> "%PS_SCRIPT%" echo         $Chunk.Clear()
>> "%PS_SCRIPT%" echo         $StartLine = $i + 1
>> "%PS_SCRIPT%" echo     }
>> "%PS_SCRIPT%" echo     $SingleHeader = @('// Connector-safe GamePanel section fragment.', '// NOT compiled source.', '// Source: src/mechanist/GamePanel.java', ('// Lines: {0}-{1}' -f $StartLine, ($i+1)), ('// Max fragment bytes: {0}' -f $MaxBytes), '')
>> "%PS_SCRIPT%" echo     $SingleBytes = ByteCount @($SingleHeader + @($Line))
>> "%PS_SCRIPT%" echo     if ($SingleBytes -gt $MaxBytes) { throw "Single source line $($i+1) exceeds connector cap with header: $SingleBytes bytes" }
>> "%PS_SCRIPT%" echo     $Chunk.Add($Line)
>> "%PS_SCRIPT%" echo }
>> "%PS_SCRIPT%" echo if ($Chunk.Count -gt 0) { Write-Fragment $Chunk $StartLine $Lines.Length }
>> "%PS_SCRIPT%" echo $Readme = @('# GamePanel Connector-Safe Sections', '', 'This directory is the direct hard split of src/mechanist/GamePanel.java into connector-readable fragments.', 'These fragments are NOT compiled source.', ('Hard cap per file: {0} bytes UTF-8, including headers.' -f $MaxBytes), ('Generated fragments: {0}' -f $Fragments.Count), '', 'Use index_*.tsv files to locate line ranges. No generated file in this directory may exceed the cap.')
>> "%PS_SCRIPT%" echo [System.IO.File]::WriteAllText((Join-Path $OutDir 'README.md'), ($Readme -join [Environment]::NewLine), $Utf8NoBom)
>> "%PS_SCRIPT%" echo $IndexRows = $Fragments ^| ForEach-Object { '{0}`t{1}`t{2}`t{3}`t{4}' -f $_.File, $_.StartLine, $_.EndLine, $_.Bytes, $_.Lines }
>> "%PS_SCRIPT%" echo $IndexHeader = 'file`tstart_line`tend_line`tbytes`tlines'
>> "%PS_SCRIPT%" echo $Batch = New-Object System.Collections.Generic.List[string]
>> "%PS_SCRIPT%" echo $Batch.Add($IndexHeader)
>> "%PS_SCRIPT%" echo $IndexPart = 1
>> "%PS_SCRIPT%" echo foreach ($Row in $IndexRows) {
>> "%PS_SCRIPT%" echo     $Probe = New-Object System.Collections.Generic.List[string]
>> "%PS_SCRIPT%" echo     foreach ($existing in $Batch) { $Probe.Add($existing) }
>> "%PS_SCRIPT%" echo     $Probe.Add($Row)
>> "%PS_SCRIPT%" echo     if ((ByteCount ([string[]]$Probe)) -gt $MaxBytes -and $Batch.Count -gt 1) {
>> "%PS_SCRIPT%" echo         $IndexName = ('index_{0:D4}.tsv' -f $IndexPart)
>> "%PS_SCRIPT%" echo         [System.IO.File]::WriteAllText((Join-Path $OutDir $IndexName), (($Batch.ToArray()) -join [Environment]::NewLine), $Utf8NoBom)
>> "%PS_SCRIPT%" echo         $IndexPart++
>> "%PS_SCRIPT%" echo         $Batch.Clear(); $Batch.Add($IndexHeader)
>> "%PS_SCRIPT%" echo     }
>> "%PS_SCRIPT%" echo     $Batch.Add($Row)
>> "%PS_SCRIPT%" echo }
>> "%PS_SCRIPT%" echo if ($Batch.Count -gt 1) { $IndexName = ('index_{0:D4}.tsv' -f $IndexPart); [System.IO.File]::WriteAllText((Join-Path $OutDir $IndexName), (($Batch.ToArray()) -join [Environment]::NewLine), $Utf8NoBom) }
>> "%PS_SCRIPT%" echo $Oversized = Get-ChildItem -LiteralPath $OutDir -File ^| Where-Object { $_.Length -gt $MaxBytes }
>> "%PS_SCRIPT%" echo if ($Oversized) { $Oversized ^| ForEach-Object { Write-Host ('OVERSIZE: {0} {1}' -f $_.FullName, $_.Length) }; throw 'Connector-safe split verification failed.' }
>> "%PS_SCRIPT%" echo Write-Host ('Split complete: {0} fragments under {1}. No file exceeds {2} bytes.' -f $Fragments.Count, $OutDir, $MaxBytes)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%"
set SPLIT_RESULT=%ERRORLEVEL%
del "%PS_SCRIPT%" >nul 2>nul
if not "%SPLIT_RESULT%"=="0" (
    echo [FAIL] GamePanel section split failed.
    pause
    exit /b %SPLIT_RESULT%
)

echo.
echo [3/4] Staging connector-safe sections...
git add RUN_SECTION_GAMEPANEL_CONNECTOR_SAFE.bat docs\gamepanel_sections

echo.
echo [4/4] Committing and pushing sections...
git commit -m "Architecture: section GamePanel into connector-safe fragments"
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
echo DONE - GamePanel was split into connector-safe section files.
echo Output: %OUTDIR%
echo Cap: %MAX_FRAGMENT_BYTES% bytes per generated file.
echo ============================================================
pause
exit /b 0
