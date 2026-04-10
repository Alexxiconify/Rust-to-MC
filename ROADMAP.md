# Rust to MC Roadmap

This document outlines the future plans and feature goals for **Rust to MC**, a performance-focused Minecraft mod that offloads heavy computations to native Rust code.

## 🚀 Core Vision

To minimize Java's overhead in Minecraft's **client-side** hot-paths by leveraging Rust's safety, speed, and SIMD capabilities via the Foreign Function & Memory (FFM) API and JNI.

---

## 📅 Short-Term Goals (1-2 Months)

### 1. Zero-Copy Map Processing

- **Status**: Hook Paused (MapRendererMixin) / Refactoring Accessor
- **Goal**: Re-implement map processing using a direct memory pointer from 1.21.11's `NativeImage` (pointer: long) instead of the broken `pixels` accessor.
- **Benefit**: Zero-allocation, zero-copy map updates bypassing the Java array heap entirely.

### 2. SIMD Frustum & Occlusion

- **Status**: Implemented (Vanilla) / Optimized (LODs)
- **Goal**: Rewrite the `isOutside` and `batchFrustumTest` in Rust using explicit SIMD instructions (SSE2/AVX2).
- **Benefit**: Near-instant culling for thousands of particles and entities per frame.

### 3. Native HUD Matrix Stack

- **Status**: Supplemented via Matrix4f.mul
- **Goal**: Create a native-backed `MatrixStack` chain calculation to offload complex HUD layout transformations.
- **Benefit**: Complements ImmediatelyFast to reduce HUD rendering overhead to absolute minimum.

### 4. JNI Bridge Bottleneck Reduction

- **Status**: **Phase 2 Complete**. Matrix Math, Bulk Lighting, and Frustum tests are now zero-copy and SIMD-optimized.
- **Goal**: Finalize `tickParticles` and `processMapTexture` pinning.
- **Benefit**: Zero Java allocation on all critical rendering hot-paths.

---

## 🛠 Medium-Term Goals (3-6 Months)

### 1. Native Chunk Meshing & Parsing

- **Status**: Planning
- **Goal**: Offload vertex buffer construction and use a native chunk decoder (PumpkinMC style) to bypass Java-side NBT parsing.
- **Benefit**: Drastically reduced "World Load" times and zero GC pressure during high-speed flight.

### 2. Starlight-Native BFS Lighting

- **Status**: Researching
- **Goal**: Replace the current bit-packed placeholder in `rustPropagateLightBulk` with a high-speed Breadth-First Search (BFS) in Rust.
- **Benefit**: Real-time lighting updates for mods like ScalableLux and Starlight, eliminating "lighting lag" during TNT explosions or terrain edits.

### 3. Native Packet Interception

- **Status**: Hook Implemented (DecoderHandlerMixin)
- **Goal**: Offload repetitive packet handling (KeepAlives, Heartbeats) to Rust to save Java main thread time and reduce allocations.
- **Benefit**: Smoother server play and reduced networking-related GC pressure.

---

## ✅ Completed & Optimized

### 1. Rendering & Math Hot-Paths

- **Native Matrix Math (SIMD)**: All `Matrix4f.mul` operations are now zero-copy and use a SIMD-friendly column-major pattern.
- **SIMD Frustum Optimization**: Batch-processes 4 planes at once to maximize vector throughput and pipeline efficiency.
- **Adaptive Particle Culling**: Intelligent throttle that relaxes when ImmediatelyFast is active and tightens when heavy entity mods (EMF/ETF) are present.
- **Parallel Map Processing**: Added `rustProcessMapTexture` logic to parallelize map color calculations for Item Frames.

### 2. Infrastructure & Build

- **Fast Build Pipeline**: Migrated from Fat LTO to **Thin LTO** and increased **codegen-units to 16** to parallelize compilation.
- **LLD Linker Integration**: Configured `rust-lld` for Windows MSVC to drastically reduce linking times.
- **SIMD Audio Suite**: Native volume scaling and stereo panning implemented using Rayon for high-frequency sound buffer manipulation.
- **Zero-Allocation Lighting Queue**: Replaced `ArrayBlockingQueue<int[]>` with primitive-backed synchronized `long[]` buffers, eliminating all per-task allocations.
- **Shared Global Frustum**: Persistent native context syncs once per frame, avoiding per-call frustum recreation.
- **Virtual Threaded Initialization**: Mod compatibility layers (DH, ScalableLux) now initialize in parallel on virtual threads.

### 3. JNI Memory Pinning (Core)

- **Zero-Copy Matrix Math**: Enabled triple-pinning for `rustMatrixMul` to eliminate float[] copies.
- **Zero-Copy Lighting**: Removed `region` copies for `propagateLightBulk` (SIMD) and `propagateLightDH`.
- **Batch Frustum Pinning**: Enabled zero-copy `aabbs` handoff for `rustBatchFrustumTest`.
- **Zero-Copy Chunk Buffers**: Enabled `NoCopyBack` pinning for large chunk data.

---

## 🔍 Feature Backlog (From Source TODOs)

### 1. Byte-Packed Navigation Grid

- **Fast Pathfinding**: Implement A* with a bit-packed navigation grid in Rust to replace expensive Java block checks.
- **Distant Horizons BFS**: Replace bit-packed LOD light task decrements with a true 3D BFS grid propagator.

---

### Last Updated: April 10, 2026*
