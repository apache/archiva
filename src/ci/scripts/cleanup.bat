@echo off
SET runpath=%~dp0

PowerShell.exe -NonInteractive -ExecutionPolicy bypass -File %runpath%cleanup.ps1 %*
