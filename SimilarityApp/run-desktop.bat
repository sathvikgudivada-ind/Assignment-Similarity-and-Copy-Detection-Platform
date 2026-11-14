@echo off
REM Compiles and starts the older desktop-window version.
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 ( pause & exit /b 1 )
java -cp "out;lib/*" SimilarityApp
