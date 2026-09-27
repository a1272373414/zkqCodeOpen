# prep_test.ps1  (ASCII only to avoid encoding issues)
# Purpose: stop game+assistant -> install latest APK -> deploy latest script JAR
#          (push only, NO broadcast, to avoid accidental trigger) -> launch assistant and
#          start the real bot Service (ControlWindowService, idle/Main) -> enter game at main village.
# NOTE: this script only prepares. After it finishes, open the clan-chat "capital friendly"
#       challenge card in CoC, then run tools/trigger_run.ps1 to start the script.
#
# Why we must start the Service explicitly:
#   DEBUG_RELOAD only reloads JAR and sets AppStateManager mode to Run; it does NOT start
#   ControlWindowService. The actual bot loop (runBot) lives in that Service and only runs in Run mode.
#   If only the broadcast is sent while the Service is dead (e.g. app/game was stopped), the script
#   can never reach runTestCode.

$ErrorActionPreference = 'Stop'

$sdkDir = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = "$sdkDir\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    Write-Host "[ERROR] adb not found: $adb"
    exit 1
}

$serial       = if ($env:ADB_SERIAL) { $env:ADB_SERIAL } else { "emulator-5556" }
$projectRoot  = "d:\work\my\zkqfz\zkqCodeOpen"
$assistantPkg = "com.coc.zkqcode"
# CoC package name on this device is the Tencent-published build, NOT com.supercell.clashofclans.
$cocPkg       = "com.tencent.tmgp.supercell.clashofclans"
$cocActivity  = "com.supercell.titan.tencent.GameAppTencent"
$serviceName  = "$assistantPkg/.core.ui.floatingwindows.ControlWindowService"
$assetsDir    = "$projectRoot\app\src\main\assets"

# 0. device online
Write-Host ""
Write-Host "=== check device $serial online ==="
& $adb -s $serial get-state
if ($LASTEXITCODE -ne 0) { throw "device not connected" }

# 1. stop game and assistant
Write-Host ""
Write-Host "=== force-stop game and assistant ==="
& $adb -s $serial shell am force-stop $cocPkg
& $adb -s $serial shell am force-stop $assistantPkg
Start-Sleep -Seconds 2

# 2. build and install APK
Set-Location $projectRoot
Write-Host ""
Write-Host "=== assemble debug APK ==="
& .\gradlew.bat :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed" }

Write-Host ""
Write-Host "=== install APK to $serial ==="
& $adb -s $serial install -r "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"
if ($LASTEXITCODE -ne 0) { throw "install APK failed" }

# 3. build and deploy script JAR (push only, no broadcast)
Write-Host ""
Write-Host "=== build script JAR ==="
& .\gradlew.bat :app:buildJar
if ($LASTEXITCODE -ne 0) { throw "buildJar failed" }

$jars = Get-ChildItem $assetsDir -Filter encrypted_*.jar | Sort-Object LastWriteTime -Descending
if ($jars.Count -eq 0) { throw "no encrypted_*.jar found in $assetsDir" }
$jar = $jars[0]
Write-Host "using JAR: $($jar.Name)"

Write-Host ""
Write-Host "=== push JAR to device (push only, no broadcast) ==="
& $adb -s $serial shell rm -f /sdcard/*.jar
if ($LASTEXITCODE -ne 0) { throw "clean /sdcard failed" }
& $adb -s $serial push $jar.FullName "/sdcard/$($jar.Name)"
if ($LASTEXITCODE -ne 0) { throw "push JAR failed" }
$cpCmd = "cp /sdcard/$($jar.Name) /data/data/com.coc.zkqcode/files/assets/$($jar.Name) && chmod 644 /data/data/com.coc.zkqcode/files/assets/$($jar.Name)"
& $adb -s $serial shell su -c "'$cpCmd'"
if ($LASTEXITCODE -ne 0) { throw "device copy JAR failed" }

# 4. launch assistant app
Write-Host ""
Write-Host "=== launch assistant app ==="
& $adb -s $serial shell monkey -p $assistantPkg -c android.intent.category.LAUNCHER 1
Start-Sleep -Seconds 4

# 5. start the real bot Service (idle/Main, will not run until Run mode)
Write-Host ""
Write-Host "=== start ControlWindowService (bot engine) ==="
& $adb -s $serial shell run-as $assistantPkg am start-service --user 0 -n $serviceName
Start-Sleep -Seconds 3

# 5.5 minimize assistant so it does not block CoC after launch
Write-Host ""
Write-Host "=== minimize assistant (send HOME) ==="
& $adb -s $serial shell input keyevent 3
Start-Sleep -Seconds 1

# 6. enter game at main village
Write-Host ""
Write-Host "=== enter game (main village) ==="
& $adb -s $serial shell am start -n "$cocPkg/$cocActivity"
if ($LASTEXITCODE -ne 0) { throw "start CoC failed" }
Start-Sleep -Seconds 6

# 7. capture a frame to confirm CoC foreground (expect 1280x720 landscape)
& $adb -s $serial shell screencap -p /sdcard/prep.png
& $adb -s $serial pull /sdcard/prep.png "$projectRoot\temp\capital_debug\prep.png"

Write-Host ""
Write-Host "[DONE] Prep ready: APK+JAR are latest, ControlWindowService started (idle/Main), CoC in foreground."
Write-Host "[NEXT] In CoC clan chat, show the capital-friendly challenge card (red ATTACK button on screen), then run:"
Write-Host "       powershell -ExecutionPolicy Bypass -File $projectRoot\tools\trigger_run.ps1"
