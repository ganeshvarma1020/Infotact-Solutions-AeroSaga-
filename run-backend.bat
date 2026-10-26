@echo off
title AeroSaga Backend (Spring Boot 3.3.4 + Temporal.io)
echo ========================================================
echo   Starting AeroSaga Backend (Spring Boot + Temporal.io)
echo ========================================================
cd /d "%~dp0backend"
call .\mvnw.cmd spring-boot:run
pause
