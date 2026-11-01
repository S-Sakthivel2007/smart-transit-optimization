$url = 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar'
$out = 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization\lib\mysql-connector-j-8.3.0.jar'
Write-Output "Downloading $url to $out ..."
Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing -TimeoutSec 30
if (Test-Path $out) {
    $item = Get-Item $out
    Write-Output "SUCCESS: $($item.Name) size $($item.Length) bytes"
} else {
    Write-Output "FAILED to download"
}
