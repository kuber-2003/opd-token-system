@echo off
echo ╔═══════════════════════════════════════════════════════════════════╗
echo ║         OPD Token Allocation System - Quick Start                 ║
echo ╚═══════════════════════════════════════════════════════════════════╝
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven is not installed. Please install Maven 3.8+ first.
    echo    Visit: https://maven.apache.org/install.html
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java is not installed. Please install Java 17+ first.
    echo    Visit: https://openjdk.org/
    pause
    exit /b 1
)

echo ✅ Prerequisites check passed
echo.

:menu
echo Choose an option:
echo 1. Run the Spring Boot API (with Swagger UI)
echo 2. Run the OPD Day Simulation
echo 3. Build the project
echo 4. Clean and rebuild
echo.
set /p choice="Enter your choice (1-4): "

if "%choice%"=="1" goto api
if "%choice%"=="2" goto simulation
if "%choice%"=="3" goto build
if "%choice%"=="4" goto clean_build
goto invalid

:api
echo.
echo 🚀 Starting Spring Boot Application...
echo 📊 Swagger UI will be available at: http://localhost:8080/swagger-ui.html
echo.
mvn spring-boot:run
goto end

:simulation
echo.
echo 🎬 Running OPD Day Simulation...
echo.
mvn compile exec:java -Dexec.mainClass="com.medoc.opd.OPDSimulation"
goto end

:build
echo.
echo 🔨 Building project...
mvn clean install
echo.
echo ✅ Build complete!
pause
goto end

:clean_build
echo.
echo 🧹 Cleaning and rebuilding...
mvn clean install -DskipTests
echo.
echo ✅ Clean build complete!
pause
goto end

:invalid
echo.
echo ❌ Invalid choice. Please run the script again and choose 1-4.
pause
goto end

:end
