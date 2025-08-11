@echo off
echo ===================================
echo CraftMusic Version Switcher
echo ===================================
echo.
echo Available versions:
echo 1. Minecraft 1.21.1 (Latest)
echo 2. Minecraft 1.20.4
echo.

set /p choice="Select version (1 or 2): "

if "%choice%"=="1" (
    echo Switching to Minecraft 1.21.1...
    git checkout 1.21.x
    copy /Y gradle.properties.1.21.1 gradle.properties 2>nul
    if not exist gradle.properties.1.21.1 (
        echo gradle.properties.1.21.1 not found, keeping current
    )
    echo Switched to 1.21.x branch
) else if "%choice%"=="2" (
    echo Switching to Minecraft 1.20.4...
    git checkout 1.20.x
    copy /Y gradle.properties.1.20.4 gradle.properties
    echo Switched to 1.20.x branch
) else (
    echo Invalid choice!
    exit /b 1
)

echo.
echo Version switched successfully!
echo Run "./gradlew build" to build the mod
pause
