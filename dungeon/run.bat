@echo off
chcp 1251 > nul
cd /d "%~dp0"

java -Dfile.encoding=windows-1251 -cp "out" Main

pause