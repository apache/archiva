@echo off
SET runpath=%~dp0

PowerShell.exe -NonInteractive -ExecutionPolicy bypass -File %runpath%src\ci\scripts\cleanup.ps1 %*
