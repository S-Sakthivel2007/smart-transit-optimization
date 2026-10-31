@echo off
setlocal
echo ===============================================================================
echo   STARTING VETRINAGAR SMART TRANSIT WEB DASHBOARD + SQLITE DATABASE
echo ===============================================================================

set PROJECT_DIR=%~dp0
cd /d "%PROJECT_DIR%"

if not exist bin mkdir bin

REM Locate Java Compiler & Runtime
set JAVAC_BIN=javac
if exist "C:\Program Files\Java\jdk-21.0.12\bin\javac.exe" (
    set "JAVAC_BIN=C:\Program Files\Java\jdk-21.0.12\bin\javac.exe"
)

set JAVA_BIN=java
if exist "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-21.0.12\bin\java.exe"
)

echo [1/3] Compiling Java source files...
"%JAVAC_BIN%" -cp "lib/*;bin" -d bin src/main/java/org/vetrinagar/model/*.java src/main/java/org/vetrinagar/graph/*.java src/main/java/org/vetrinagar/policy/*.java src/main/java/org/vetrinagar/db/*.java src/main/java/org/vetrinagar/service/*.java src/main/java/org/vetrinagar/cli/*.java src/main/java/org/vetrinagar/web/*.java src/main/java/org/vetrinagar/*.java

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Verified SQLite Relational Database: vetrinagar_transit.db
echo [3/3] Opening Web Dashboard at http://localhost:8080/ ...

REM Open default browser to the web application
start "" http://localhost:8080/

echo ===============================================================================
echo  WEBSITE RUNNING LIVE AT: http://localhost:8080/
echo  Press Ctrl+C in this window when you wish to stop the transit web server.
echo ===============================================================================

"%JAVA_BIN%" -cp "lib/*;bin" org.vetrinagar.Main --web

pause
