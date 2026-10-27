@echo off
setlocal
set ROOT=%~dp0
"%ROOT%runtime\bin\java.exe" -cp "%ROOT%packages\server\TheMechanistServer.jar;%ROOT%packages\support\lib\*" mechanist.MechanistServerMain %*
