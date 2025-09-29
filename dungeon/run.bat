@echo off
setlocal
set ROOT=%~dp0
set OUT=%ROOT%out

chcp 65001 > nul

java -Dfile.encoding=UTF-8 -cp "%OUT%" Main

pause