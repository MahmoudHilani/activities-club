@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0dev-stop.ps1" %*
endlocal
