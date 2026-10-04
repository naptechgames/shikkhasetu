# ShikkhaSetu - development environment for THIS PowerShell session only.
# Usage (note the leading dot):   . .\tools\env.ps1
# Nothing is written to the Windows system/user environment variables.

$DevTools = 'E:\DevTools'

$jdk = Get-ChildItem $DevTools -Directory -Filter 'jdk-17*' | Select-Object -First 1
if (-not $jdk) { throw "JDK 17 not found in $DevTools" }

$env:JAVA_HOME        = $jdk.FullName
$env:ANDROID_SDK_ROOT = "$DevTools\android-sdk"
$env:ANDROID_HOME     = $env:ANDROID_SDK_ROOT
# Keep the big download caches on E: instead of C:
$env:GRADLE_USER_HOME = "$DevTools\gradle-home"
$env:PUB_CACHE        = "$DevTools\pub-cache"

$env:Path = @(
    "$env:JAVA_HOME\bin",
    "$DevTools\flutter\bin",
    "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin",
    "$env:ANDROID_SDK_ROOT\platform-tools",
    $env:Path
) -join ';'

Write-Host "JAVA_HOME        = $env:JAVA_HOME"
Write-Host "ANDROID_SDK_ROOT = $env:ANDROID_SDK_ROOT"
Write-Host "Flutter          = $DevTools\flutter"
