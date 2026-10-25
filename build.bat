@echo off
REM ==============================================================================
REM Mumbai Metro Smart Card & Fare Ticketing System
REM Spring Boot Web Application - Windows Batch Build & Execution Script
REM SE Semester 3 (OOPM / Mini Project 1A)
REM ==============================================================================

setlocal enabledelayedexpansion

set APP_NAME=mumbai-metro-ticketing
set APP_VERSION=1.0.0
set MAIN_CLASS=com.mumbaimetro.MetroApplication
set TEST_CLASS=com.mumbaimetro.test.MetroSystemTest
set TARGET_DIR=target
set CLASSES_DIR=%TARGET_DIR%\classes
set JAR_PATH=%TARGET_DIR%\%APP_NAME%-%APP_VERSION%.jar

echo ========================================================================
echo    MUMBAI METRO ONE (MMOPL) - SPRING BOOT WINDOWS BUILD SCRIPT          
echo    Target: Java 17+ ^| Spring Boot 3.3.5 ^| Thymeleaf & Pure-Java QR      
echo ========================================================================
echo.

REM Check Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] java is not installed or not in your PATH!
    echo Please install JDK 17 or higher from https://adoptium.net/
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
where npm >nul 2>nul
if %errorlevel% equ 0 (
    if exist "frontend\package.json" (
        echo [FRONTEND] Building React SPA with Vite...
        cd frontend && call npm run build && cd ..
    )
)
echo [INFO] Compiling and packaging Spring Boot application with Maven...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed!
    exit /b 1
)
echo [SUCCESS] Standalone Executable Spring Boot JAR packaged: %JAR_PATH%
exit /b 0

:do_run
if not exist "%JAR_PATH%" (
    echo [WARN] Application JAR not found. Building first...
    call :do_build
)
echo [LAUNCH] Starting Mumbai Metro Spring Boot Web Application...
echo [INFO] Access the Web Dashboard at: http://localhost:8080
echo [INFO] Press Ctrl+C to terminate the server.
echo.
java -jar "%JAR_PATH%"
exit /b 0

:do_test
echo [INFO] Compiling test classes...
call mvn compile -q
echo [TEST] Running automated academic test suite (%TEST_CLASS%)...
java -cp "lib\*;%CLASSES_DIR%" "%TEST_CLASS%"
exit /b %errorlevel%

:do_clean
echo [INFO] Cleaning project build artifacts...
call mvn clean
if exist dist rmdir /s /q dist
del /q /f Ticket_*.txt QR_*.png metro_system_data.dat 2>nul
echo [SUCCESS] Workspace cleaned successfully!
exit /b 0

:show_help
echo Usage: build.bat [command]
echo.
echo Commands:
echo   build       Compile and package the Spring Boot executable JAR (Default)
echo   run         Build and launch the Spring Boot Web server on port 8080
echo   test        Run the automated academic verification test suite (9 checks)
echo   clean       Remove build directory and temporary files
echo   help        Display this help message
echo.
echo Examples:
echo   build.bat          # Builds the Spring Boot JAR
echo   build.bat run      # Launches http://localhost:8080
echo   build.bat test     # Executes automated test suite
exit /b 0
