@echo off
REM 固定打包并安装到模拟器 emulator-5556（遵循项目 AGENTS.md 的 Debug Environment Rules）
cd /d %~dp0..
setlocal enabledelayedexpansion

REM 解析 Android SDK 路径：优先 ANDROID_HOME 环境变量，否则用默认安装位置 %LOCALAPPDATA%\Android\Sdk
if defined ANDROID_HOME (set "SDK_DIR=%ANDROID_HOME%") else (set "SDK_DIR=%LOCALAPPDATA%\Android\Sdk")

set "ADB=%SDK_DIR%\platform-tools\adb.exe"
if not exist "%ADB%" (
  echo [错误] 未找到 adb：%ADB%
  echo 请在 local.properties 设置正确的 sdk.dir，或安装 Android SDK。
  exit /b 1
)

echo [1/2] 构建 debug 包...
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo [错误] 构建失败
  exit /b 1
)

echo [2/2] 安装到 emulator-5556...
"%ADB%" -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
  echo [错误] 安装失败
  exit /b 1
)
echo 完成：已构建并安装到 emulator-5556
endlocal
