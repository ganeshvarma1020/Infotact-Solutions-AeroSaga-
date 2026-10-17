@echo off
title AeroSaga Master Launcher
echo ========================================================
echo   Launching AeroSaga Drone Mission Control Platform
echo ========================================================
echo.
echo 1. Launching Spring Boot + Temporal.io Backend...
start "AeroSaga Backend" cmd /k ""%~dp0run-backend.bat""

echo 2. Launching React 18 + Vite Frontend...
start "AeroSaga Frontend" cmd /k ""%~dp0run-frontend.bat""

echo 3. Waiting for services to initialize...
timeout /t 6 /nobreak > nul

echo 4. Opening Mission Control Dashboard in your browser...
start http://localhost:5173
echo.
echo ========================================================
echo   AeroSaga is running! Keep the command windows open.
echo   UI: http://localhost:5173
echo   Swagger: http://localhost:8080/swagger-ui/index.html
echo ========================================================
exit
