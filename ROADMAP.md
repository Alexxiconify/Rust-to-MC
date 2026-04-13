# Rust to MC Roadmap

This document outlines the future plans and feature goals for **Rust to MC**, a performance-focused Minecraft mod that offloads heavy computations to native Rust code.

## 🚀 Core Vision

To minimize Java's overhead in Minecraft's **client-side** hot-paths by leveraging Rust's safety, speed, and SIMD capabilities via the Foreign Function & Memory (FFM) API and JNI.

To minimize Java's overhead in Minecraft's **client-side** hot-paths by leveraging Rust's safety, speed, and SIMD capabilities via the Foreign Function & Memory (FFM) API and JNI.

## Rollback Notes (v1.0.3-a3)

- **Rollback target**: `v1.0.3-a3` (`12e8073`)
- **Rolled back from**: `main` at `9cd2790`
- **Commits removed from working branch**: 13 commits (`v1.0.3-a3..9cd2790`)
- **Backup branch saved**: `backup/pre-rollback-v1.0.3-a3-20260412-210316`

### What changed after v1.0.3-a3 (for clean reimplementation)

- **Total delta**: 83 files changed, 2438 insertions, 1519 deletions
- **New tooling/docs added**: `.agents/skills/*`, `CLAUDE.md`, `skills-lock.json`, `docs/future-compat-mods.md`
- **Build/config touched**: `build.gradle`, `settings.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `versions/*/build.gradle`, `rust_mc_core` submodule pointer
- **Core bridge/runtime heavily modified**: `NativeBridge`, `ModBridge`, `RustMC`, `PreLaunchHandler`, `MixinManager`, and multiple compat classes
- **Mixins significantly changed**: broad edits across rendering/network/pathfinding mixins, one deletion (`CommandManagerMixin`), one addition (`RandomMixin`)
- **UI/config paths changed**: `ModMenuIntegration`, `RustMCConfig`, and render overlays/util classes (`BlameLog`, `RenderState`, etc.)
- **Native binary changed**: `src/main/resources/rust_mc_core.dll` (size and contents changed)

### Reimplementation order (recommended)

1. Re-apply **build system + wrapper + submodule pointer** changes first.
2. Re-apply **NativeBridge/ModBridge/RustMC lifecycle** changes and validate startup.
3. Re-apply **config + ModMenu/UI overlays** changes.
4. Re-apply **mixin changes in small batches**, testing after each group.
5. Re-apply **native DLL update last**, then run full in-game sanity checks.

### Saved diff artifacts

- `docs/rollback/commits_since_v1.0.3-a3.txt`
- `docs/rollback/name_status_since_v1.0.3-a3.txt`
- `docs/rollback/diff_stat_since_v1.0.3-a3.txt`
- `docs/rollback/full_diff_since_v1.0.3-a3.patch`
- `docs/rollback/working_tree_uncommitted.patch`

---

## 📅 Short-Term Goals (1-2 Months)

### 1. SIMD Frustum & Occlusion
- **Benefit**: Real-time lighting updates for mods like ScalableLux and Starlight, eliminating "lighting lag" during TNT explosions or terrain edits.
- **Status**: Implemented (Vanilla) / Optimized (LODs)
- **Goal**: Rewrite the `isOutside` and `batchFrustumTest` in Rust using explicit SIMD instructions (SSE2/AVX2).
- **Benefit**: Near-instant culling for thousands of particles and entities per frame.
### 3. Native Packet Interception
### 2. Native HUD Matrix Stack

- **Status**: Supplemented via Matrix4f.mul
- **Goal**: Create a native-backed `MatrixStack` chain calculation to offload complex HUD layout transformations.
- **Benefit**: Complements ImmediatelyFast to reduce HUD rendering overhead to absolute minimum.

---
- **Native Packet Handler**: Offload NBT parsing and serialization for high-traffic network packets.
## 🛠 Medium-Term Goals (3-6 Months)

### 1. Native Chunk Meshing & Parsing
## ✅ Completed & Optimized
- **Status**: Planning
- **Goal**: Offload vertex buffer construction and use a native chunk decoder (PumpkinMC style) to bypass Java-side NBT parsing.
- **Benefit**: Drastically reduced "World Load" times and zero GC pressure during high-speed flight.
### 1. JNI Memory Pinning (Core)
### 2. Starlight-Native BFS Lighting
- **Adaptive Particle Culling**: Intelligent throttle that relaxes when ImmediatelyFast is active and tightens when heavy entity mods (EMF/ETF) are present.
- **Status**: Researching
- **Goal**: Replace the current bit-packed placeholder in `rustPropagateLightBulk` with a high-speed Breadth-First Search (BFS) in Rust.
- **Benefit**: Real-time lighting updates for mods like ScalableLux and Starlight, eliminating "lighting lag" during TNT explosions or terrain edits.
- **Fast Build Pipeline**: Migrated to **Thin LTO**, parallel codegen, and robust change detection; disabled incremental release builds to prevent cache corruption.
### 3. Native Packet Interception
- **SIMD Audio Suite**: Native volume scaling and stereo panning implemented using Rayon for high-frequency sound buffer manipulation.
- **Status**: Hook Implemented (DecoderHandlerMixin)
- **Goal**: Offload repetitive packet handling (KeepAlives, Heartbeats) to Rust to save Java main thread time and reduce allocations.
- **Benefit**: Smoother server play and reduced networking-related GC pressure.
---

### Last Updated: April 10, 2026*
## 🔍 Feature Backlog (From Source TODOs)

- **Fast JSON Bridge**: Replace GSON with `serde_json` for resource/language loading.
- **ModMenu Statistics**: Expose native JNI/SIMD metrics directly in the UI.
- **Native Packet Handler**: Offload NBT parsing and serialization for high-traffic network packets.
- **Distant Horizons BFS**: Replace bit-packed LOD light task decrements with a true 3D BFS grid propagator.

---

## ✅ Completed & Optimized

### 1. JNI Memory Pinning (Core)

- **Critical JNI Pinning**: Enabled `get_array_elements_critical` for all high-frequency hot-paths including `tickParticles`, `rustMatrixMul`, and `rustTransformVertices`.
- **Zero-Copy Map Processing**: Subverted `int[]` copies by using 1.21's `NativeImage` pointer directly in Rust.
- **Zero-Copy Matrix Math**: Enabled triple-pinning for `rustMatrixMul` to eliminate float[] copies.
- **Zero-Copy Lighting**: Removed `region` copies for `propagateLightBulk` (SIMD) and `propagateLightDH`.
- **Batch Frustum Pinning**: Enabled zero-copy `aabbs` handoff for `rustBatchFrustumTest`.
- **Zero-Copy Chunk Buffers**: Enabled `NoCopyBack` pinning for large chunk data.

### 2. Rendering & Math Hot-Paths

- **Native Matrix Math (SIMD)**: All `Matrix4f.mul` operations are now zero-copy and use a SIMD-friendly column-major pattern.
- **SIMD Frustum Optimization**: Batch-processes 4 planes at once to maximize vector throughput and pipeline efficiency.
- **Adaptive Particle Culling**: Intelligent throttle that relaxes when ImmediatelyFast is active and tightens when heavy entity mods (EMF/ETF) are present.
- **Parallel Map Processing**: Added `rustProcessMapTexture` logic to parallelize map color calculations for Item Frames.
- **Hardware Sqrt (SIMD)**: Replaced magic numbers with native `RSQRTSS` intrinsics for core math paths.
- **Adaptive Frustum Culling**: Fixed 'aggressive' culling by incorporating `fov_scale` and normalizing AABB bounds in native code.

### 3. Infrastructure & Build

- **Fast Build Pipeline**: Migrated to **Thin LTO**, parallel codegen, and robust change detection; disabled incremental release builds to prevent cache corruption.
- **LLD Linker Integration**: Configured `rust-lld` for Windows MSVC to drastically reduce linking times.
- **SIMD Audio Suite**: Native volume scaling and stereo panning implemented using Rayon for high-frequency sound buffer manipulation.
- **Zero-Allocation Lighting Queue**: Replaced `ArrayBlockingQueue<int[]>` with primitive-backed synchronized `long[]` buffers, eliminating all per-task allocations.
- **Shared Global Frustum**: Persistent native context syncs once per frame, avoiding per-call frustum recreation.
- **Virtual Threaded Initialization**: Mod compatibility layers (DH, ScalableLux) now initialize in parallel on virtual threads.
- **Ultra-Fast Startup**: Backgrounded WGPU initialization and DNS cache loading; removed blocking JNI/Compat joins during `onInitialize`.
- **Zero-Warning Base**: Fixed all major Clippy and Java IDE warnings in the core bridge logic.
- **Persistent Lib Cache**: Drastically reduced bootstrap time by caching native binaries in the config folder.
- **Zero-Alloc Inflation**: Implemented `rustInflateRaw` for high-throughput, allocation-free world decompression.

---

### Last Updated: April 12, 2026*