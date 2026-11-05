@echo off
title AeroSaga Master Launcher (Official Guide Ref ITS/JD/1144)
echo ========================================================
echo   Launching AeroSaga Drone Mission Control Platform
echo ========================================================
echo.
echo Starting AeroSaga Application on Port 8080...
cd /d "%~dp0official\aerosaga"
start "AeroSaga Mission Control" cmd /k "mvnw.cmd spring-boot:run"

echo Waiting for AeroSaga backend to initialize (approx 20-25 seconds)...
powershell -Command "for ($i = 0; $i -lt 40; $i++) { Start-Sleep -Seconds 1; try { $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/ping' -TimeoutSec 1; if ($r.status -eq 'UP') { break } } catch {} }"

echo Opening Mission Control Dashboard in your browser...
start http://localhost:8080
echo.
echo ========================================================
echo   AeroSaga is running! Keep the command window open.
echo   URL:      http://localhost:8080
echo   User:     operator
echo   Password: local-dev-only
echo ========================================================
exit
