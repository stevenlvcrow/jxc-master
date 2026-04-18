@echo off
pushd "%~dp0"
powershell -ExecutionPolicy Bypass -NoLogo -NoProfile -File ".\dev.ps1" -Action Start
popd
