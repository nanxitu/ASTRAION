@echo off
title ASTRAION Server
color 0B

:: Disable QuickEdit & Insert Mode (prevents freeze on click)
reg add HKCU\Console /v QuickEdit /t REG_DWORD /d 0 /f >nul 2>&1
reg add HKCU\Console /v InsertMode /t REG_DWORD /d 1 /f >nul 2>&1

echo ========================================
echo          A S T R A I O N
echo       Star Forge - AI Manages Everything
echo ========================================
echo.

set JAR_FILE=%~dp0astraion-server-1.0.0-SNAPSHOT.jar

if not exist "%JAR_FILE%" (
    echo [ERROR] JAR not found: %JAR_FILE%
    pause
    exit /b 1
)

echo [INFO] Checking port 8080...
netstat -ano | findstr /C:":8080 " | findstr "LISTENING" > "%temp%\astraion_port.txt"
set /p PORT_PID=<"%temp%\astraion_port.txt"
if defined PORT_PID (
    for /f "tokens=5" %%a in ("%temp%\astraion_port.txt") do (
        echo [INFO] Killing old process PID=%%a
        taskkill /PID %%a /F >nul 2>&1
        timeout /t 2 >nul
    )
)
del "%temp%\astraion_port.txt" 2>nul
echo [INFO] Port 8080 is free.
echo.

echo [INFO] Starting ASTRAION server...
echo.
java -jar "%JAR_FILE%"
echo.
pause
