@echo off
chcp 65001 > nul
cd /d "C:\Users\Segunfai\IdeaProjects\dungeon"

echo Проверка структуры...
echo.
echo Main.java:
type src\Main.java
echo.
echo Пакет Game.java:
findstr "package" src\core\Game.java
echo.
echo Импорты Game.java:
findstr "import" src\core\Game.java

pause