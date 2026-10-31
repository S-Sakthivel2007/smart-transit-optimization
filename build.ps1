Set-Location 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization'
$javaFiles = Get-ChildItem -Recurse -Filter '*.java' -Path 'src/main/java' | ForEach-Object { $_.FullName }
& 'C:\Program Files\Java\jdk-21.0.12\bin\javac.exe' -d bin -cp 'lib/*' -sourcepath src/main/java @javaFiles
if ($LASTEXITCODE -eq 0) {
    Write-Host "BUILD SUCCESS"
} else {
    Write-Host "BUILD FAILED with exit code $LASTEXITCODE"
}
