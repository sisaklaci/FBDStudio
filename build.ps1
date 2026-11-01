# Builds FBD Studio and packages it as a native Windows app (FBDStudio.exe) via jpackage.
# Usage: powershell -ExecutionPolicy Bypass -File build.ps1

$ErrorActionPreference = "Stop"

$JavaHome = "C:\SISAK\tools\jdk-21.0.12.1+1"
$MavenHome = "C:\Program Files\apache-maven-3.9.16"
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$MavenHome\bin;$env:PATH"

$root = $PSScriptRoot
Set-Location $root

Write-Host "==> mvn clean package" -ForegroundColor Cyan
& "$MavenHome\bin\mvn.cmd" -q clean package
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

$jpackageInput = Join-Path $root "target\jpackage-input"
$jpackageDist  = Join-Path $root "target\dist"

if (Test-Path $jpackageInput) { Remove-Item $jpackageInput -Recurse -Force }
if (Test-Path $jpackageDist)  { Remove-Item $jpackageDist -Recurse -Force }
New-Item -ItemType Directory -Path $jpackageInput | Out-Null

Copy-Item (Join-Path $root "target\fbd-studio-1.0.0-all.jar") $jpackageInput

Write-Host "==> jpackage (app-image)" -ForegroundColor Cyan
& "$JavaHome\bin\jpackage.exe" `
    --type app-image `
    --input $jpackageInput `
    --dest $jpackageDist `
    --name FBDStudio `
    --app-version 1.0.0 `
    --main-jar fbd-studio-1.0.0-all.jar `
    --main-class com.sisak.fbdstudio.app.Launcher `
    --vendor "SISAK"

if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Write-Host ""
Write-Host "Done. Launch: $jpackageDist\FBDStudio\FBDStudio.exe" -ForegroundColor Green
