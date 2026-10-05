param(
    [string]$Serial = 'emulator-5554',
    [string]$Output = 'validation/production-ad-diagnostics.log'
)
$ErrorActionPreference = 'Stop'
$taskSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$taskAdb = Join-Path $taskSdk 'platform-tools/adb.exe'
# Read-only capture: never clears logs, changes device settings or clicks an advertisement.
& $taskAdb -s $Serial logcat -d -v threadtime 'MergeSevenAds:D' 'ConsentManager:I' 'UserMessagingPlatform:D' 'Ads:I' '*:S' |
    Set-Content -LiteralPath $Output -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw 'ADB logcat failed' }
Write-Output "Ad diagnostics saved to $Output"
