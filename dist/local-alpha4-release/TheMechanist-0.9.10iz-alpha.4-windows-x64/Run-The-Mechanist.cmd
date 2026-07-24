@echo off
setlocal
set ROOT=%~dp0
"%ROOT%runtime\bin\java.exe" -jar "%ROOT%launcher\MechanistLauncher.jar" %*
