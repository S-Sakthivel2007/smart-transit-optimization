@echo off
setlocal
echo ===============================================================================
echo   Compiling and Running Vetrinagar Comprehensive Automated Test Suite
echo ===============================================================================

set PROJECT_DIR=%~dp0
cd /d "%PROJECT_DIR%"

if not exist bin mkdir bin

REM Locate Java Compiler
set JAVAC_BIN=javac
if exist "C:\Program Files\Java\jdk-21.0.12\bin\javac.exe" (
    set "JAVAC_BIN=C:\Program Files\Java\jdk-21.0.12\bin\javac.exe"
)

REM Locate Java Runtime
set JAVA_BIN=java
if exist "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-21.0.12\bin\java.exe"
)

echo [1/2] Compiling main sources and test suite...
"%JAVAC_BIN%" -cp "lib/*;bin" -d bin src/main/java/org/vetrinagar/model/*.java src/main/java/org/vetrinagar/graph/*.java src/main/java/org/vetrinagar/policy/*.java src/main/java/org/vetrinagar/db/*.java src/main/java/org/vetrinagar/service/*.java src/main/java/org/vetrinagar/cli/*.java src/main/java/org/vetrinagar/web/*.java src/main/java/org/vetrinagar/*.java src/test/java/org/vetrinagar/*.java

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Test compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Executing SmartTransitTest...
"%JAVA_BIN%" -cp "lib/*;bin" org.vetrinagar.SmartTransitTest

pause
