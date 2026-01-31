#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════════════╗"
echo "║         OPD Token Allocation System - Quick Start                 ║"
echo "╚═══════════════════════════════════════════════════════════════════╝"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.8+ first."
    echo "   Visit: https://maven.apache.org/install.html"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17+ first."
    echo "   Visit: https://openjdk.org/"
    exit 1
fi

echo "✅ Prerequisites check passed"
echo ""

# Display menu
echo "Choose an option:"
echo "1. Run the Spring Boot API (with Swagger UI)"
echo "2. Run the OPD Day Simulation"
echo "3. Build the project"
echo "4. Clean and rebuild"
echo ""
read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        echo ""
        echo "🚀 Starting Spring Boot Application..."
        echo "📊 Swagger UI will be available at: http://localhost:8080/swagger-ui.html"
        echo ""
        mvn spring-boot:run
        ;;
    2)
        echo ""
        echo "🎬 Running OPD Day Simulation..."
        echo ""
        mvn compile exec:java -Dexec.mainClass="com.medoc.opd.OPDSimulation"
        ;;
    3)
        echo ""
        echo "🔨 Building project..."
        mvn clean install
        echo ""
        echo "✅ Build complete!"
        ;;
    4)
        echo ""
        echo "🧹 Cleaning and rebuilding..."
        mvn clean install -DskipTests
        echo ""
        echo "✅ Clean build complete!"
        ;;
    *)
        echo ""
        echo "❌ Invalid choice. Please run the script again and choose 1-4."
        exit 1
        ;;
esac
