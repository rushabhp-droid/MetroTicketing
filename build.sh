#!/usr/bin/env bash
# ==============================================================================
# Mumbai Metro Smart Card & Fare Ticketing System
# Build & Execution Automation Script
# Mumbai University - SE Semester 3 (OOPM / Mini Project 1A)
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
MAIN_CLASS="com.mumbaimetro.Main"
TEST_CLASS="com.mumbaimetro.test.MetroSystemTest"
TARGET_DIR="target"
CLASSES_DIR="${TARGET_DIR}/classes"
JAR_PATH="${TARGET_DIR}/${APP_NAME}-${APP_VERSION}.jar"

print_header() {
    echo -e "${C_CYAN}${C_BOLD}"
    echo "========================================================================"
    echo "   MUMBAI METRO ONE (MMOPL) - BUILD SCRIPT                              "
    echo "   Target: Java 17+ | Swing GUI & Pure-Java QR Subsystem               "
    echo "========================================================================"
    echo -e "${C_RESET}"
}

check_jdk() {
    if ! command -v javac &> /dev/null || ! command -v java &> /dev/null; then
        echo -e "${C_RED}[ERROR] JDK (javac / java) is not installed or not in PATH!${C_RESET}"
        echo "Please install JDK 17 or higher (e.g., sudo pacman -S jdk-openjdk or sudo apt install openjdk-17-jdk)."
        exit 1
    fi
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo -e "${C_BLUE}[INFO]${C_RESET} Found Java environment: ${JAVA_VER}"
}

compile_pure_javac() {
    echo -e "${C_BLUE}[INFO]${C_RESET} Compiling source code using direct javac..."
    mkdir -p "${CLASSES_DIR}"
    
    # Collect all Java source files
    mapfile -t SOURCES < <(find src/main/java -name "*.java")
    
    if [ ${#SOURCES[@]} -eq 0 ]; then
        echo -e "${C_RED}[ERROR] No Java sources found in src/main/java!${C_RESET}"
        exit 1
    fi

    # Unpack all library JARs into classes directory to produce a self-contained Fat JAR
    for jar_file in lib/*.jar; do
        if [ -f "$jar_file" ]; then
            echo -e "${C_BLUE}[INFO]${C_RESET} Bundling $(basename "$jar_file") into application..."
            (cd "${CLASSES_DIR}" && jar xf "../../$jar_file")
        fi
    done

    javac -encoding UTF-8 -cp "lib/*:${CLASSES_DIR}" -d "${CLASSES_DIR}" "${SOURCES[@]}"
    echo -e "${C_GREEN}[SUCCESS]${C_RESET} Compiled ${#SOURCES[@]} Java classes into ${CLASSES_DIR}/"
    
    # Package into standalone executable Fat JAR
    echo -e "${C_BLUE}[INFO]${C_RESET} Creating standalone executable JAR (${JAR_PATH})..."
    jar cfe "${JAR_PATH}" "${MAIN_CLASS}" -C "${CLASSES_DIR}" .
    echo -e "${C_GREEN}[SUCCESS]${C_RESET} Standalone Executable JAR packaged: ${JAR_PATH}"
}

do_build() {
    check_jdk
    compile_pure_javac
}

do_run() {
    if [ ! -f "${JAR_PATH}" ]; then
        echo -e "${C_YELLOW}[WARN]${C_RESET} Application JAR not found. Triggering build first..."
        do_build
    fi
    
    echo -e "${C_CYAN}[LAUNCH]${C_RESET} Starting Mumbai Metro Ticketing System..."
    java -jar "${JAR_PATH}"
}

do_test() {
    check_jdk
    if [ ! -d "${CLASSES_DIR}/com/mumbaimetro/test" ]; then
        echo -e "${C_BLUE}[INFO]${C_RESET} Compiling project classes first..."
        compile_pure_javac
    fi
    
    echo -e "${C_CYAN}[TEST]${C_RESET} Running automated test suite (${TEST_CLASS})..."
    java -cp "lib/*:${CLASSES_DIR}" "${TEST_CLASS}"
}

do_clean() {
    echo -e "${C_BLUE}[INFO]${C_RESET} Cleaning build artifacts and runtime temporary files..."
    rm -rf "${TARGET_DIR}" dist/
    rm -f Ticket_*.txt QR_*.png metro_system_data.dat
    echo -e "${C_GREEN}[SUCCESS]${C_RESET} Workspace cleaned successfully!"
}

do_native() {
    check_jdk
    if [ ! -f "${JAR_PATH}" ]; then
        do_build
    fi
    if ! command -v jpackage &> /dev/null; then
        echo -e "${C_RED}[ERROR] jpackage is not found in your JDK installation!${C_RESET}"
        exit 1
    fi
    echo -e "${C_CYAN}[NATIVE]${C_RESET} Building native desktop application image using jpackage..."
    rm -rf dist/
    jpackage --type app-image \
        --name "${APP_NAME}" \
        --input "${TARGET_DIR}" \
        --main-jar "${APP_NAME}-${APP_VERSION}.jar" \
        --main-class "${MAIN_CLASS}" \
        --dest dist
    echo -e "${C_GREEN}[SUCCESS]${C_RESET} Native standalone executable created at: dist/${APP_NAME}/bin/${APP_NAME}"
}

show_help() {
    echo "Usage: ./build.sh [command]"
    echo ""
    echo "Commands:"
    echo "  build       Compile all sources and generate the executable JAR (Default)"
    echo "  run         Build (if needed) and run the application"
    echo "  test        Run the automated test suite (8 verification checks)"
    echo "  native      Build a standalone native ELF binary using jpackage"
    echo "  clean       Remove build directory and temporary files"
    echo "  help        Display this help message"
    echo ""
    echo "Examples:"
    echo "  ./build.sh          # Builds the project"
    echo "  ./build.sh run      # Runs the Swing GUI on desktop"
    echo "  ./build.sh test     # Validates OOP rules, exceptions, & QR engine"
    echo "  ./build.sh native   # Creates native binary: dist/mumbai-metro-ticketing/bin/..."
}

# Entrypoint logic
print_header

case "${1:-build}" in
    build)
        do_build
        ;;
    run)
        do_run
        ;;
    test)
        do_test
        ;;
    native)
        do_native
        ;;
    clean)
        do_clean
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${C_RED}[ERROR] Unknown command: '$1'${C_RESET}"
        show_help
        exit 1
        ;;
esac
