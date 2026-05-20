@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set LIBS=lib\*
set SRC=pos-server\src
set OUT=pos-server\out

if not exist "%OUT%" mkdir "%OUT%"

:: Find javac — prefer JetBrains Runtime bundled with IntelliJ Toolbox
set JAVAC=javac
set JAVA=java

set "JBR_BASE=%LOCALAPPDATA%\JetBrains\Toolbox\apps\intellij-idea"
if exist "%JBR_BASE%" (
    for /d %%v in ("%JBR_BASE%\ch-0\*") do (
        if exist "%%v\jbr\bin\javac.exe" (
            set "JAVAC=%%v\jbr\bin\javac.exe"
            set "JAVA=%%v\jbr\bin\java.exe"
        )
    )
)

echo Compiling server...
dir /s /b "%SRC%\*.java" > "%TEMP%\server_sources.txt"
"%JAVAC%" -cp "%LIBS%" -d "%OUT%" @"%TEMP%\server_sources.txt"

if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b 1
)

echo Starting server on port 9090...
"%JAVA%" -cp "%OUT%;%LIBS%" server.POSServer
pause
