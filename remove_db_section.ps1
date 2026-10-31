$f = 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization\src\main\resources\web\index.html'
$lines = Get-Content $f
$newLines = $lines[0..537] + $lines[590..($lines.Length-1)]
$newLines | Set-Content $f -Encoding UTF8
Write-Host "Done. Removed database explorer section (lines 539-590)."
