@echo off
title Push AeroSaga to GitHub
echo ========================================================
echo   Pushing AeroSaga to GitHub Repository
echo   Repo: https://github.com/ganeshvarma1020/Infotact-Solutions-AeroSaga-
echo ========================================================
echo.
cd /d "%~dp0"

echo [1/3] Checking Git Status...
git status

echo.
echo [2/3] Adding any new files...
git add .

echo.
echo [3/3] Pushing to GitHub (origin main)...
echo If a GitHub Sign-in window pops up, click "Sign in with your browser".
echo.
git push -u origin main --force

echo.
echo ========================================================
echo   Push Complete! Refresh your GitHub repository page:
echo   https://github.com/ganeshvarma1020/Infotact-Solutions-AeroSaga-
echo ========================================================
pause
