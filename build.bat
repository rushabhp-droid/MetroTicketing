@echo off
REM ==============================================================================
REM Mumbai Metro Smart Card & Fare Ticketing System
REM Windows Batch Build & Execution Script
REM Mumbai University - SE Semester 3 (OOPM / Mini Project 1A)
REM ==============================================================================

setlocal enabledelayedexpansion

set APP_NAME=mumbai-metro-ticketing
set APP_VERSION=1.0.0
set MAIN_CLASS=com.mumbaimetro.Main
set TEST_CLASS=com.mumbaimetro.test.MetroSystemTest
set TARGET_DIR=target
set CLASSES_DIR=%TARGET_DIR%\classes
set JAR_PATH=%TARGET_DIR%\%APP_NAME%-%APP_VERSION%.jar

echo ========================================================================
echo    MUMBAI METRO ONE (MMOPL) - WINDOWS BUILD SCRIPT                      
echo    Target: Java 17+ ^| Swing GUI & Pure-Java QR Subsystem               
echo ========================================================================
echo.

REM Check Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] java is not installed or not in your PATH!
    echo Please install JDK 17 or higher from https://adoptium.net/
    exit /b 1
)

where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] javac compiler is not installed or not in your PATH!
    exit /b 1
)

set ACTION=%1
if "%ACTION%"=="" set ACTION=build

if /I "%ACTION%"=="build" goto do_build
if /I "%ACTION%"=="run" goto do_run
if /I "%ACTION%"=="test" goto do_test
if /I "%ACTION%"=="clean" goto do_clean
if /I "%ACTION%"=="help" goto show_help

echo [ERROR] Unknown command: %ACTION%
goto show_help

:do_build
echo [INFO] Creating output directory %CLASSES_DIR%...
if not exist "%CLASSES_DIR%" mkdir "%CLASSES_DIR%"

echo [INFO] Scanning Java source files...
dir /s /B src\main\java\*.java > sources.txt

echo [INFO] Compiling Java classes with javac...
javac -encoding UTF-8 -cp "lib\*;%CLASSES_DIR%" -d "%CLASSES_DIR%" @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    exit /b %errorlevel%
)
echo [SUCCESS] Compilation completed successfully!

echo [INFO] Unpacking library dependencies into Fat JAR...
for %%f in (lib\*.jar) do (
    echo [INFO] Bundling %%~nxf...
    pushd "%CLASSES_DIR%"
    jar xf "..\..\%%f"
    popd
)

echo [INFO] Packaging executable JAR (%JAR_PATH%)...
jar cfe "%JAR_PATH%" %MAIN_CLASS% -C "%CLASSES_DIR%" .
if %errorlevel% neq 0 (
    echo [ERROR] JAR packaging failed!
    exit /b %errorlevel%
)
echo [SUCCESS] Executable JAR created: %JAR_PATH%
exit /b 0

:do_run
if not exist "%JAR_PATH%" (
    echo [WARN] JAR not found. Triggering build first...
    call :do_build
)
echo [LAUNCH] Launching Mumbai Metro Ticketing System...
java -jar "%JAR_PATH%"
exit /b 0

:do_test
if not exist "%CLASSES_DIR%\com\mumbaimetro\test\MetroSystemTest.class" (
    call :do_build
)
echo [TEST] Running automated test suite...
java -cp "lib\*;%CLASSES_DIR%" %TEST_CLASS%
exit /b 0

:do_clean
echo [INFO] Cleaning target directory and temporary receipts...
if exist "%TARGET_DIR%" rmdir /s /q "%TARGET_DIR%"
del /q Ticket_*.txt QR_*.png metro_system_data.dat 2>nul
echo [SUCCESS] Cleaned!
exit /b 0

:show_help
echo Usage: build.bat [build ^| run ^| test ^| clean ^| help]
echo.
echo Commands:
echo   build       Compile sources and generate executable JAR (Default)
echo   run         Build and launch the application
echo   test        Run the automated test suite
echo   clean       Delete target directory and runtime files
echo   help        Show this guide
exit /b 0
