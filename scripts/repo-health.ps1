Write-Host "Running repo health checks"

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $root

New-Item -ItemType Directory -Path .\health -Force | Out-Null

Write-Host "Running cargo check..."
Push-Location .\rust_mc_core
cargo check --all-targets 2>&1 | Tee-Object -FilePath ..\health\cargo-check.txt

Write-Host "Running cargo clippy..."
cargo clippy --all-targets --all-features -- -D warnings 2>&1 | Tee-Object -FilePath ..\health\cargo-clippy.txt
Pop-Location

Set-Location $root

Write-Host "Running gradle build... (this may take a while)"
.\gradlew clean build --no-daemon --stacktrace 2>&1 | Tee-Object -FilePath .\health\gradle-build.txt

Write-Host "Collected health outputs in .\health\"