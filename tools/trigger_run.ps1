# trigger_run.ps1  (ASCII only)
# Prereq: run tools/prep_test.ps1 first, and in CoC clan chat show the capital-friendly
#         challenge card (red ATTACK button must be on screen; otherwise runTestCode ends
#         immediately because no card is found).
# Action: send DEBUG_RELOAD broadcast -> reload JAR and set AppStateManager mode to Run.
#         The already-running ControlWindowService observes Run and starts
#         runBot -> runMainScript -> runTestCode.

$ErrorActionPreference = 'Stop'

$sdkDir = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = "$sdkDir\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { Write-Host "[ERROR] adb not found: $adb"; exit 1 }

$serial = if ($env:ADB_SERIAL) { $env:ADB_SERIAL } else { "emulator-5556" }

Write-Host "=== send DEBUG_RELOAD to trigger script ==="
& $adb -s $serial shell am broadcast -a com.coc.zkqcode.DEBUG_RELOAD -n com.coc.zkqcode/.core.system.daemon.DebugReloadReceiver
if ($LASTEXITCODE -ne 0) { throw "broadcast failed" }
Write-Host ""
Write-Host "[DONE] Triggered. ControlWindowService will detect the ATTACK card on screen and enter battle."
