[CmdletBinding()]
param(
    [string]$Serial = "",
    [switch]$OpenLeakTests
)
$ErrorActionPreference = "Stop"
$adb = (Get-Command adb -ErrorAction Stop).Source
$target = @()
if ($Serial.Trim()) { $target = @("-s", $Serial.Trim()) }

function Invoke-AndroidAdb {
    param([string[]]$Arguments)
    $output = & $adb @target @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb command failed. Verify that your own Android device is authorized."
    }
    return $output
}

Write-Host "ByMeVPN Android — non-destructive device acceptance helper"
Write-Host "This checks device metadata only. It does NOT prove DNS, IPv6 or VPN leak protection."
$deviceInfo = Invoke-AndroidAdb -Arguments @("shell", "getprop", "ro.build.version.release")
Write-Host ("Android version: " + ($deviceInfo | Select-Object -First 1))
$install = @(Invoke-AndroidAdb -Arguments @("shell", "pm", "path", "com.bymevpn.app"))
if (-not ($install | Where-Object { $_ -match "^package:" })) {
    throw "ByMeVPN is not installed on the selected device."
}
Write-Host "ByMeVPN: installed"
$packageDump = @(Invoke-AndroidAdb -Arguments @("shell", "dumpsys", "package", "com.bymevpn.app"))
$packageDump | Select-String -Pattern "versionName=|versionCode=" | Select-Object -First 2
$connectivity = @(Invoke-AndroidAdb -Arguments @("shell", "dumpsys", "connectivity"))
$vpnLines = @($connectivity | Select-String -Pattern "TRANSPORT_VPN|type: VPN|VPN.*Network|VpnNetworkAgent" | Select-Object -First 8)
if ($vpnLines.Count -gt 0) {
    Write-Host "VPN network-related system entries (not end-to-end proof):"
    $vpnLines | ForEach-Object { Write-Host $_.Line.Trim() }
} else {
    Write-Host "No VPN system entries found. Connect in ByMeVPN and re-run this helper."
}

if ($OpenLeakTests) {
    Write-Host "Open each page in a separate browser application while VPN is active."
    Write-Host "Compare the results with the disconnected state; this step requires human verification."
    foreach ($site in @("https://ipleak.net", "https://www.dnsleaktest.com", "https://test-ipv6.com")) {
        Write-Host ("Opening " + $site)
        Invoke-AndroidAdb -Arguments @("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", $site) | Out-Null
    }
}
Write-Host "Done. No subscription URLs, keys or traffic contents were collected."
