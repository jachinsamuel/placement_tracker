Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "   CAMPUS PLACEMENT SHORTLISTING SYSTEM - LAUNCHER" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[*] Checking Java environment..." -ForegroundColor Yellow

try {
    $javaVer = & java -version 2>&1
    Write-Host "[+] Java found: $($javaVer[0])" -ForegroundColor Green
} catch {
    Write-Host "[X] Java is not installed or not in PATH!" -ForegroundColor Red
    Write-Host "Please install Java 21+ and add it to your system PATH." -ForegroundColor Red
    exit 1
}

# Free port 8080 if already occupied by an old background process
$portCheck = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portCheck) {
    Write-Host "[!] Port 8080 is currently in use by PID $($portCheck.OwningProcess[0]). Releasing..." -ForegroundColor Yellow
    Stop-Process -Id $portCheck.OwningProcess[0] -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

$jarPath = "target\placement-portal-1.0.0.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "[*] JAR file not found. Building project with Maven..." -ForegroundColor Yellow
    & mvn clean package -DskipTests
}

Write-Host "`n[*] Starting Placement Portal with Interactive Terminal Console & Web UI..." -ForegroundColor Green
Write-Host "[*] Web Portal will be live at: http://localhost:8080" -ForegroundColor Cyan
Write-Host "[*] Interactive Terminal Console starting below...`n" -ForegroundColor Cyan

& java -jar target\placement-portal-1.0.0.jar --spring.profiles.active=dev
