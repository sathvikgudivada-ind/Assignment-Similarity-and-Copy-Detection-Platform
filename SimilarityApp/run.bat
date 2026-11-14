@echo off
REM Compiles and starts the website version (opens in your browser).
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
  echo.
  echo Compile failed. Check that Java JDK is installed: javac -version
  pause
  exit /b 1
)
java -cp "out;lib/*" SimilarityWeb
pause
