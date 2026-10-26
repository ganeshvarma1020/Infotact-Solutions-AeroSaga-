@echo off
title AeroSaga Frontend (React + Vite)
echo ========================================================
echo   Starting AeroSaga React Frontend (Vite)
echo ========================================================
cd /d "%~dp0frontend"
call npm.cmd run dev -- --host
pause
