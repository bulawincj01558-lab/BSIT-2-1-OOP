@echo off
rem Starts Liceo U Go Automate from the built jar. Build it first with: mvn clean package
cd /d "%~dp0"
if not exist "target\liceo-u-go-automate.jar" (
    echo The app is not built yet. Run "mvn clean package" in this folder first.
    pause
    exit /b 1
)
start "" javaw -jar "target\liceo-u-go-automate.jar"
