@echo off

chcp 65001 > nul
setlocal enabledelayedexpansion
set ROOT=%~dp0
set OUT=%ROOT%out

echo Компиляция DungeonMini...

rmdir /s /q "%OUT%" 2>nul
mkdir "%OUT%"

dir /s /b "%ROOT%src\*.java" > "%ROOT%.sources"

javac -encoding UTF-8 -d "%OUT%" @"%ROOT%.sources"

if !errorlevel! equ 0 (
    echo ✅ Build OK. Run run.bat
    del "%ROOT%.sources" 2>nul
) else (
    echo ❌ Build failed!
    del "%ROOT%.sources" 2>nul
    pause
    exit /b 1
)