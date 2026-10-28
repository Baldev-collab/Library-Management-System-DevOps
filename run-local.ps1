# PowerShell runner for Library Management System with in-memory H2 database
Write-Host "Starting Library Management System on port 8082 (H2 in-memory)..." -ForegroundColor Cyan
if (-not (Test-Path "target\library-management-system-1.0.0.jar")) {
    Write-Host "Target JAR not found. Packaging with Maven..." -ForegroundColor Yellow
    mvn clean package -DskipTests
}

java -jar target\library-management-system-1.0.0.jar `
    --server.port=8082 `
    '--spring.datasource.url=jdbc:h2:mem:librarydb;DB_CLOSE_DELAY=-1;MODE=MySQL' `
    --spring.datasource.driver-class-name=org.h2.Driver `
    --spring.datasource.username=sa `
    --spring.datasource.password=""
