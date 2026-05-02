#+ Profiling & Artifact Collection (Windows PowerShell)

Purpose: step-by-step commands and helper-script usage to collect profiling artifacts (Rust native, JVM, and ETW) on Windows. Use these artifacts to locate hotspots and validate remediation work described in `ROADMAP.md`.

Prerequisites

- Windows 10/11 with PowerShell (this guide is written for PowerShell). Run profiling commands from repository root.
- Rust toolchain (matching MSRV; project currently uses 1.89+)
- Java 21+ (JDK with jcmd/jps/jfr available)
- Gradle wrapper (project includes `gradlew.bat`)
- Windows Performance Recorder (WPR) / Windows Performance Analyzer (WPA) for ETW traces (optional admin)

Quick repo checks

```powershell
Set-Location 'C:\Users\Taylor Allred\Documents\Files\projects\Rust to MC'
.\gradlew dependencies --configuration runtimeClasspath > gradle-deps.txt
Set-Location .\rust_mc_core
cargo tree --all-features > ..\cargo-tree.txt
```

Static analysis

```powershell
# From rust_mc_core
cargo check --all-targets
cargo clippy --all-targets --all-features -- -D warnings

# Optional (install once):
cargo install cargo-bloat
cargo bloat --release --crates > ..\cargo-bloat.txt

# From repo root
Set-Location 'C:\Users\Taylor Allred\Documents\Files\projects\Rust to MC'
.\gradlew clean build --no-daemon --stacktrace > gradle-build.txt
```

Build-for-profiling (Rust native)

Temporarily build with debug info and CPU-targeted optimizations. Close this PowerShell session afterward to clear RUSTFLAGS.

```powershell
Set-Location .\rust_mc_core
# $env:RUSTFLAGS='-C debuginfo=2 -C target-cpu=native'
$env:RUSTFLAGS='-C debuginfo=2 -C opt-level=3 -C target-cpu=native'
cargo build --release
```

JVM Flight Recorder (JFR)

Start the game with JFR enabled (add to Fabric run config or launch wrapper). Example: add JVM arg to launch process.

```text
-XX:StartFlightRecording=filename=./jfr/record.jfr,duration=60s,settings=profile
```

Or attach at runtime (choose PID from `jps`)

```powershell
jps -v
# then
jcmd <pid> JFR.start name=profile settings=profile filename=record.jfr
# stop later
jcmd <pid> JFR.stop name=profile
```

Windows ETW trace (WPR/WPA)

Use admin PowerShell for full kernel+CPU stacks. Short capture (30–60s) while reproducing target scene.

```powershell
# Start capture (Admin)
wpr -start CPU -filemode
# reproduce scenario in-game
# Stop and write ETL
wpr -stop rustmc.etl
```

Collect logs and artifacts

```powershell
# Make sure folders exist
New-Item -ItemType Directory -Path .\jfr -Force | Out-Null
New-Item -ItemType Directory -Path .\profiling -Force | Out-Null

# Copy artifacts to profiling/ for bundling
Copy-Item -Path '..\cargo-tree.txt' -Destination .\profiling\ -Force
Copy-Item -Path '..\cargo-bloat.txt' -Destination .\profiling\ -Force -ErrorAction SilentlyContinue
Copy-Item -Path '..\gradle-deps.txt' -Destination .\profiling\ -Force -ErrorAction SilentlyContinue
Copy-Item -Path '.\target\release\rust_mc_core.dll' -Destination .\profiling\ -Force -ErrorAction SilentlyContinue
Copy-Item -Path '..\logs\latest.log' -Destination .\profiling\ -Force -ErrorAction SilentlyContinue
Copy-Item -Path '.\jfr\record.jfr' -Destination .\profiling\ -Force -ErrorAction SilentlyContinue
Copy-Item -Path '.\rustmc.etl' -Destination .\profiling\ -Force -ErrorAction SilentlyContinue

# Zip bundle
Compress-Archive -Path .\profiling\* -DestinationPath .\profiling-artifacts.zip -Force
```

Helper scripts

- `scripts\collect-profiling.ps1` — build-with-symbols, start WPR for configurable seconds, stop and bundle artifacts.
- `scripts\repo-health.ps1` — run `cargo check`, `cargo clippy`, `./gradlew build` and collect outputs.

Checklist before sharing artifacts

- Run `cargo clippy` and `gradlew build` and attach results
- Produce at least one JFR (30–60s) focused on suspected hotspot scenario
- Produce one short ETW trace (30–60s) on Windows (Admin recommended)
- Bundle `profiling-artifacts.zip` and upload for review

Runtime diagnostic logger

The mod includes a low-overhead diagnostics thread that emits Java-side timing counters when `DiagnosticMode` is set to `TIMING` or `ALL` in the mod config (`rust-mc.json` or via ModMenu). The logger runs every 5s and writes to the game log (visible in `logs/latest.log`) with keys for:

- frustumCalls, frustumTotalNs
- particleCalls, particleTotalNs
- dhFusedCalls, dhFusedTotalNs
- chunkAttempts, forwards, failures, avgIngestMicros

Enable it before reproducing a scenario to collect representative timing samples.

Estimated times & risks

- Static checks: 15–30m
- Build-for-profiling (Rust): 10–30m (depends on machine)
- JFR capture: 1–5m each
- ETW capture and symbolication: 30–90m

Risks: profiling build flags may change inlining/optimizations; interpret results with care and repeat with different compile flags if needed.

---

Last updated: May 1, 2026