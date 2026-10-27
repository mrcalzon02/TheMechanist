@echo off
setlocal
set ROOT=%~dp0
"%ROOT%runtime\bin\java.exe" -cp "%ROOT%packages\client\TheMechanist.jar;%ROOT%packages\support\lib\*" mechanist.RemoteClientMain %*
