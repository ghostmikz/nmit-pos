@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set LIBS=lib\*
set SRC=pos-client\src
set OUT=pos-client\out

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

echo Compiling client...
dir /s /b "%SRC%\*.java" > "%TEMP%\client_sources.txt"
"%JAVAC%" -cp "%LIBS%" -d "%OUT%" @"%TEMP%\client_sources.txt"

if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b 1
)

:: Copy resource files (properties, images) to out directory
echo Copying resources...
for /r "%SRC%" %%f in (*.properties *.png *.jpg *.svg *.gif) do (
    set "REL=%%f"
    set "REL=!REL:%CD%\%SRC%\=!"
    set "DEST=%OUT%\!REL!"
    for %%d in ("!DEST!") do if not exist "%%~dpd" mkdir "%%~dpd"
    copy /y "%%f" "!DEST!" >nul
)

echo Starting client...
"%JAVA%" ^
    -Dawt.useSystemAAFontSettings=lcd ^
    -Dswing.aatext=true ^
    -cp "%OUT%;%LIBS%" App
pause
