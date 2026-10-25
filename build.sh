#!/usr/bin/env bash
# ==============================================================================
# Mumbai Metro Smart Card & Fare Ticketing System
# Spring Boot Web Application - Build & Execution Script
# SE Semester 3 (OOPM / Mini Project 1A)
# ==============================================================================

set -e

# Terminal Colors
C_RESET='\033[0m'
C_BOLD='\033[1m'
C_GREEN='\033[32m'
C_BLUE='\033[34m'
C_CYAN='\033[36m'
C_YELLOW='\033[33m'
C_RED='\033[31m'

APP_NAME="mumbai-metro-ticketing"
APP_VERSION="1.0.0"
MAIN_CLASS="com.mumbaimetro.MetroApplication"
TEST_CLASS="com.mumbaimetro.test.MetroSystemTest"
TARGET_DIR="target"
CLASSES_DIR="${TARGET_DIR}/classes"
JAR_PATH="${TARGET_DIR}/${APP_NAME}-${APP_VERSION}.jar"

print_header() {
    echo -e "${C_CYAN}${C_BOLD}"
    echo "========================================================================"
    echo "   MUMBAI METRO ONE (MMOPL) - SPRING BOOT BUILD SCRIPT                 "
    echo "   Target: Java 17+ | Spring Boot 3.3.5 | Thymeleaf & Pure-Java QR      "
    echo "========================================================================"
    echo -e "${C_RESET}"
}

check_environment() {
    if ! command -v java &> /dev/null; then
        echo -e "${C_RED}[ERROR] java is not installed or not in PATH!${C_RESET}"
        echo "Please install JDK 17 or higher (e.g., sudo pacman -S jdk-openjdk or sudo apt install openjdk-17-jdk)."
        exit 1
    fi
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo -e "${C_BLUE}[INFO]${C_RESET} Found Java environment: ${JAVA_VER}"

    if command -v mvn &> /dev/null; then
        MVN_CMD="mvn"
    elif [ -f "./mvnw" ]; then
        MVN_CMD="./mvnw"
    else
        echo -e "${C_RED}[ERROR] Neither 'mvn' nor Maven Wrapper './mvnw' was found in PATH!${C_RESET}"
        exit 1
    fi
}

do_build() {
    check_environment
    if command -v npm &> /dev/null && [ -d "frontend" ]; then
        echo -e "${C_CYAN}[FRONTEND]${C_RESET} Building React SPA frontend with Vite..."
        (cd frontend && npm run build) || echo -e "${C_YELLOW}[WARN]${C_RESET} Frontend build skipped; using existing prebuilt static assets."
    fi
    echo -e "${C_BLUE}[INFO]${C_RESET} Compiling and packaging Spring Boot application with Maven..."
    ${MVN_CMD} clean package -DskipTests
    echo -e "${C_GREEN}[SUCCESS]${C_RESET} Executable Spring Boot Fat JAR packaged: ${JAR_PATH}"
}

do_run() {
    check_environment
    if [ ! -f "${JAR_PATH}" ]; then
        echo -e "${C_YELLOW}[WARN]${C_RESET} Application JAR not found. Triggering build first..."
        do_build
    fi
    
    echo -e "${C_CYAN}[LAUNCH]${C_RESET} Starting Mumbai Metro Spring Boot Web Application..."
    echo -e "${C_GREEN}[INFO]${C_RESET} Access the Web Dashboard at: ${C_BOLD}http://localhost:8080${C_RESET}"
    echo -e "${C_BLUE}[INFO]${C_RESET} Press Ctrl+C to stop the application."
    echo ""
    java -jar "${JAR_PATH}"
}

do_test() {
    check_environment
    if [ ! -d "${CLASSES_DIR}/com/mumbaimetro/test" ]; then
        echo -e "${C_BLUE}[INFO]${C_RESET} Compiling project classes first..."
        ${MVN_CMD} compile
    fi
    
    echo -e "${C_CYAN}[TEST]${C_RESET} Running automated academic test suite (${TEST_CLASS})..."
    java -cp "lib/*:${CLASSES_DIR}" "${TEST_CLASS}"
}

do_clean() {
    check_environment
    echo -e "${C_BLUE}[INFO]${C_RESET} Cleaning build artifacts and runtime temporary files..."
    ${MVN_CMD} clean
    rm -rf dist/
    rm -f Ticket_*.txt QR_*.png metro_system_data.dat
    echo -e "${C_GREEN}[SUCCESS]${C_RESET} Workspace cleaned successfully!"
}

show_help() {
    echo "Usage: ./build.sh [command]"
    echo ""
    echo "Commands:"
    echo "  build       Compile and package the Spring Boot executable JAR (Default)"
    echo "  run         Build (if needed) and run the Spring Boot Web server on port 8080"
    echo "  test        Run the automated academic verification test suite (9 checks)"
    echo "  clean       Remove build directory and temporary files"
    echo "  help        Display this help message"
    echo ""
    echo "Examples:"
    echo "  ./build.sh          # Builds the Spring Boot JAR"
    echo "  ./build.sh run      # Launches http://localhost:8080"
    echo "  ./build.sh test     # Executes automated test suite"
    echo "  ./build.sh clean    # Cleans build target"
}

print_header

case "$1" in
    build|"")
        do_build
        ;;
    run)
        do_run
        ;;
    test)
        do_test
        ;;
    clean)
        do_clean
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${C_RED}[ERROR] Unknown command: $1${C_RESET}"
        show_help
        exit 1
        ;;
esac
