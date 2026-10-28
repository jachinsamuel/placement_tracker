@echo off
setlocal enabledelayedexpansion
title Campus Placement Drive Portal - Interactive Terminal and Web Server
echo ============================================================================
echo   CAMPUS PLACEMENT SHORTLISTING SYSTEM - LAUNCHER
echo ============================================================================
echo.
echo [*] Checking Java environment...
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [X] Java not found in PATH!
    echo Please ensure Java 21 or higher is installed and added to PATH.
    echo.
    pause
    exit /b 1
)

echo [+] Java detected.
echo.

:: Check if port 8080 is currently in use
netstat -ano | findstr :8080 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [!] Port 8080 appears to be in use.
    echo Releasing port 8080 to ensure a clean startup...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 1 >nul 2>&1
)

:: Check if JAR exists, else build it
if not exist "target\placement-portal-1.0.0.jar" (
    echo [!] JAR file not found. Building project with Maven...
    call mvn clean package -DskipTests
    if %ERRORLEVEL% NEQ 0 (
        echo [X] Maven build failed.
        pause
        exit /b 1
    )
)

echo [*] Starting Placement Portal...
echo [*] Web Portal will be live at: http://localhost:8080
echo [*] Interactive Terminal Console starting below:
echo.
java -jar target\placement-portal-1.0.0.jar --spring.profiles.active=dev

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Application stopped.
    pause
)
