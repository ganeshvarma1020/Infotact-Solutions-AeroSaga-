@echo off
title AeroSaga Drone Mission Control - Launcher
echo =====================================================================
echo    AeroSaga: Temporal.io Autonomous Drone Mission Control
echo =====================================================================
echo.
cd /d "C:\Users\ganes\Desktop\Infotact Sloutions Project"

set "JAVA_CMD=java"
if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe"
) else if exist "C:\Program Files\Java\jdk-26\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-26\bin\java.exe"
)

echo Starting AeroSaga Mission Control Server on Port 8080...
echo [NOTE] Please keep this command window open while using the dashboard.
echo.

:: Automatically poll port 8080 and launch browser as soon as server is ready (usually 3 seconds)
start /b "" powershell -Command "for ($i = 0; $i -lt 35; $i++) { Start-Sleep -Seconds 1; try { $r = (Invoke-WebRequest -Uri 'http://localhost:8080/api/dashboard/summary' -TimeoutSec 1 -UseBasicParsing).StatusCode; if ($r -eq 200) { Start-Process 'http://localhost:8080/'; break } } catch {} }"

if exist "target\aerosaga-drone-mission-control-1.0.0.jar" (
    "%JAVA_CMD%" -jar "target\aerosaga-drone-mission-control-1.0.0.jar"
) else (
    call .\mvnw.cmd spring-boot:run
)

echo.
echo Server stopped.
pause
