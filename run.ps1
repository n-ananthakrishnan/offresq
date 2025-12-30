#!/usr/bin/env pwsh
# OffResq GPS Gateway - Quick Start Script
# This script builds and runs the Spring Boot application

Write-Host "`n"
Write-Host "╔════════════════════════════════════════════════════════╗"
Write-Host "║     OffResq GPS Gateway - Quick Start                  ║"
Write-Host "║     Spring Boot + Leaflet.js Map Visualization        ║"
Write-Host "╚════════════════════════════════════════════════════════╝"
Write-Host "`n"

# Check if Maven is installed
try {
    mvn --version | Out-Null
}
catch {
    Write-Host "❌ Maven is not installed or not in PATH" -ForegroundColor Red
    Write-Host "   Please install Maven: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    Write-Host "   Or add Maven to your PATH environment variable" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check if Java is installed
try {
    java -version 2>&1 | Out-Null
}
catch {
    Write-Host "❌ Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "   Please install Java 21+: https://www.oracle.com/java/technologies/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "✅ Java and Maven found" -ForegroundColor Green
Write-Host "`n"

# Display current directory
$location = Get-Location
Write-Host "📁 Working directory: $location"
Write-Host "`n"

# Build the project
Write-Host "🔨 Building project with Maven..."
mvn clean package -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "✅ Build successful!" -ForegroundColor Green
Write-Host "`n"

# Run the application
Write-Host "🚀 Starting Spring Boot application..." -ForegroundColor Cyan
Write-Host "   Server will run on: http://localhost:8080"
Write-Host "   Web UI: http://localhost:8080/"
Write-Host "   API: http://localhost:8080/api/locations"
Write-Host "   H2 Console: http://localhost:8080/h2-console"
Write-Host "`n"
Write-Host "⏹️  Press Ctrl+C to stop the server"
Write-Host "`n"

java -jar target/gateway-0.0.1-SNAPSHOT.jar

Read-Host "Press Enter to exit"
