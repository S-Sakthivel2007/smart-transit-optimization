Set-Location 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization'

# Copy web resources to bin directory
$webSrc = 'src\main\resources\web'
$webDest = 'bin\web'
if (Test-Path $webDest) { Remove-Item $webDest -Recurse -Force }
Copy-Item $webSrc $webDest -Recurse
Write-Host "Web resources copied to bin/web"

# Copy all resources
$resSrc = 'src\main\resources'
$resDest = 'bin'
Get-ChildItem -Path $resSrc -File | ForEach-Object {
    Copy-Item $_.FullName $resDest -Force
}
Write-Host "Resources copied."
