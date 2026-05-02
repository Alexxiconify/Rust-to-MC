param(
    [int]$DurationSeconds = 60,
    [string]$OutDir = "profiling",
    [string]$WprProfile = "CPU"
)

Write-Host "Collecting profiling artifacts (Duration: $DurationSeconds s)"

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $root

# Build rust with symbols for profiling (temporary)
Write-Host "Building rust_mc_core with debug info..."
Push-Location .\rust_mc_core
$env:RUSTFLAGS='-C debuginfo=2 -C opt-level=3 -C target-cpu=native'
cargo build --release | Tee-Object -FilePath ..\$OutDir\cargo-build.txt
Pop-Location

# Ensure output dir
New-Item -ItemType Directory -Path .\$OutDir -Force | Out-Null

# Start WPR (requires Admin for full traces)
Write-Host "Starting WPR profile: $WprProfile (requires Admin)"
wpr -start $WprProfile -filemode

Write-Host "Sleeping $DurationSeconds seconds to collect trace..."
Start-Sleep -Seconds $DurationSeconds

Write-Host "Stopping WPR and writing rustmc.etl"
wpr -stop rustmc.etl

Write-Host "Collecting files into $OutDir"
Copy-Item -Path '.\rust_mc_core\target\release\rust_mc_core.dll' -Destination .\$OutDir\ -ErrorAction SilentlyContinue
Copy-Item -Path '.\rust_mc_core\Cargo.lock' -Destination .\$OutDir\ -ErrorAction SilentlyContinue
Copy-Item -Path '.\rust_mc_core\Cargo.toml' -Destination .\$OutDir\ -ErrorAction SilentlyContinue
Copy-Item -Path '.\rust_mc_core\..\cargo-tree.txt' -Destination .\$OutDir\ -ErrorAction SilentlyContinue
Copy-Item -Path '.\rustmc.etl' -Destination .\$OutDir\ -ErrorAction SilentlyContinue
Copy-Item -Path '.\logs\latest.log' -Destination .\$OutDir\ -ErrorAction SilentlyContinue
Copy-Item -Path '.\docs\profiling.md' -Destination .\$OutDir\ -ErrorAction SilentlyContinue

Compress-Archive -Path .\$OutDir\* -DestinationPath .\profiling-artifacts.zip -Force

Write-Host "Profiling collection complete. Bundle: profiling-artifacts.zip"