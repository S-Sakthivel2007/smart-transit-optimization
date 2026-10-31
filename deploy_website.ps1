$src = 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization\src\main\resources\web'
$dst = 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization\website'
if (!(Test-Path $dst)) { New-Item -ItemType Directory -Path $dst | Out-Null }
Copy-Item "$src\*" $dst -Recurse -Force
Write-Host "Website deployed to $dst successfully."
