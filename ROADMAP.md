# Rust to MC Roadmap

This document outlines the future plans and feature goals for **Rust to MC**, a performance-focused Minecraft mod that offloads heavy computations to native Rust code.

## 🚀 Core Vision

To minimize Java's overhead in Minecraft's **client-side** hot-paths by leveraging Rust's safety, speed, and SIMD capabilities via the Foreign Function & Memory (FFM) API and JNI.

---

## 🛠 Medium-Term Goals (3-6 Months)

### 1. Native Chunk Meshing & Parsing

- **Status**: Planning
- **Goal**: Offload vertex buffer construction and use a native chunk decoder (PumpkinMC style) to bypass Java-side NBT parsing.
- **Benefit**: Drastically reduced "World Load" times and zero GC pressure during high-speed flight.

### 2. Starlight-Native BFS Lighting

- **Status**: ⚡ In Progress (1D-packed BFS partially implemented)
- **Goal**: Replace the current bit-packed placeholder in `rustPropagateLightBulk` with a high-speed Breadth-First Search (BFS) in Rust.
- **Benefit**: Massive reduction in light-update stutters during world-gen or large-scale TNT blasts.

### 3. Native Packet Interception

- **Status**: ⚡ In Progress (KeepAlive filtering implemented)
- **Goal**: Offload repetitive packet handling (KeepAlives, Heartbeats) to Rust to save Java main thread time and reduce allocations.
- **Benefit**: Smoother server play and reduced networking-related GC pressure.

---

## 🔍 Feature Backlog (From Source TODOs)

- **Fast JSON Bridge**: Replace GSON with `serde_json` for resource/language loading.
- **Native Packet Handler**: Offload NBT parsing and serialization for high-traffic network packets.
- **Distant Horizons BFS**: Replace bit-packed LOD light task decrements with a true 3D BFS grid propagator.

---

## ✅ Completed & Optimized

### 1. JNI Memory Pinning (Core)

- **Zero-Copy Memory Pinning**: Critical JNI pinning for all high-frequency hot-paths.
- **Zero-Copy Map Processing**: Subverted `int[]` copies by using 1.21's `NativeImage` pointer directly in Rust.
- **Zero-Copy Matrix Math**: Enabled triple-pinning for `rustMatrixMul` to eliminate float[] copies.
- **Zero-Copy Lighting**: Removed `region` copies for `propagateLightBulk` (SIMD) and `propagateLightDH`.
- **Batch Frustum Pinning**: Enabled zero-copy `aabbs` handoff for `rustBatchFrustumTest`.
- **Zero-Copy Chunk Buffers**: Enabled `NoCopyBack` pinning for large chunk data.

### 2. Rendering & Math Hot-Paths

- **SIMD Frustum (SSE2)**: Explicitly vectorized point/plane tests for near-instant culling.
- **Native HUD Matrix Stack**: Chain multiplication for HUD/Model hierarchies; minimizes JNI roundtrips.
- **Native Matrix Math (SIMD)**: All `Matrix4f.mul` operations are zero-copy and use a SIMD-friendly column-major pattern.
- **Adaptive Particle Culling**: Intelligent throttle that relaxes when ImmediatelyFast is active and tightens when heavy entity mods (EMF/ETF) are present.
- **Parallel Map Processing**: Added `rustProcessMapTexture` logic to parallelize map color calculations for Item Frames.
- **Hardware Sqrt (SIMD)**: Replaced magic numbers with native `RSQRTSS` intrinsics for core math paths.
- **Absolute World-Space Culling**: Fixed native frustum logic to correctly handle absolute world coordinates for Distant Horizons while preserving high precision using camera-relative internal math.
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

### 4. Logic & Style

- **Native PRNG (Xoshiro256++)**: High-speed native random number generator (Xoshiro256++) integrated via `RandomMixin` to offload `Xoroshiro128PlusPlusRandom` and `LocalRandom`.
- **Caveman Documentation Protocol**: Stripped all non-essential grammar from `GEMINI.md` and converted all multi-line Javadoc comments to compact `//` comments across the entire codebase to maximize token efficiency and code density.
- **Native Trig Pre-warming**: Optimized startup latency by pre-calculating and pre-warming the Sine/Cosine LUT on a background thread during `JNI_OnLoad`.
- **ModMenu Statistics (JNI/SIMD Metrics)**: Implemented atomic native counters and `NativeStatsRenderer` HUD to expose JNI call volume, lighting updates, and frustum test counts directly in-game.

---

### Last Updated: April 11, 2026*
