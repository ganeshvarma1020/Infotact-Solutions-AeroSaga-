@echo off
title AeroSaga Backend (Spring Boot 3.3.4 + Temporal.io)
echo ========================================================
echo   Starting AeroSaga Backend (Spring Boot + Temporal.io)
echo ========================================================
echo [1/2] Checking MySQL 8.0 Service...
net start MySQL80 >nul 2>&1
echo [2/2] Launching Spring Boot Backend...
cd /d "%~dp0backend"
call .\mvnw.cmd spring-boot:run
pause
