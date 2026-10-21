@echo off
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Please install JDK 21 or newer and add its bin folder to PATH.
  pause
  exit /b 1
)
if not exist out mkdir out
javac -encoding UTF-8 -d out src\TaskApp.java
if errorlevel 1 exit /b 1
java -cp out TaskApp
