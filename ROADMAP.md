# Rust to MC Roadmap

This document outlines the future plans and feature goals for **Rust to MC**, a performance-focused Minecraft mod that offloads heavy computations to native Rust code.

## 🚀 Core Vision

To minimize Java's overhead in Minecraft's **client-side** hot-paths by leveraging Rust's safety, speed, and SIMD capabilities via the Foreign Function & Memory (FFM) API and JNI.

---

## 📅 Short-Term Goals (1-2 Months)

### 1. Zero-Copy Map Processing

- **Status**: Hook Implemented (MapRendererMixin)
- **Goal**: Move from array-based processing to direct memory address manipulation for map textures.
- **Benefit**: Zero-allocation map updates, making high-count item frame maps (like server hubs) lag-free even at high resolutions.

### 2. SIMD Frustum & Occlusion

- **Status**: Implemented (Vanilla) / Optimized (LODs)
- **Goal**: Rewrite the `isOutside` and `batchFrustumTest` in Rust using explicit SIMD instructions (SSE2/AVX2).
- **Benefit**: Near-instant culling for thousands of particles and entities per frame.

### 3. Native HUD Matrix Stack

- **Status**: Supplemented via Matrix4f.mul
- **Goal**: Create a native-backed `MatrixStack` chain calculation to offload complex HUD layout transformations.
- **Benefit**: Complements ImmediatelyFast to reduce HUD rendering overhead to absolute minimum.

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

- **Native Matrix Math (SIMD)**: All `Matrix4f.mul` operations are now offloaded to Rust SIMD. Benefits both Vanilla and Sodium pipelines.
- **Adaptive Particle Culling**: Intelligent throttle that relaxes when ImmediatelyFast is active and tightens when heavy entity mods (EMF/ETF) are present.
- **SIMD Frustum Optimization**: Fully unrolled native frustum intersection tests to maximize scalar throughput and pipeline efficiency.
- **Parallel Map Processing**: Added `rustProcessMapTexture` logic to parallelize map color calculations for Item Frames.

### 2. Infrastructure & Audio

- **SIMD Audio Suite**: Native volume scaling and stereo panning implemented using Rayon for high-frequency sound buffer manipulation.
- **Zero-Allocation Lighting Queue**: Replaced `ArrayBlockingQueue<int[]>` with primitive-backed synchronized `long[]` buffers, eliminating all per-task allocations.
- **Shared Global Frustum**: Persistent native context syncs once per frame, avoiding per-call frustum recreation.
- **Virtual Threaded Initialization**: Mod compatibility layers (DH, ScalableLux) now initialize in parallel on virtual threads.

---

## 🔍 Feature Backlog (From Source TODOs)

### 1. Native Packet Subversion

- **Fast Packet Decoder**: Implement native packet deserialization to bypass Java reflection in `PacketDeflaterMixin`.
- **Distant Horizons BFS**: Replace bit-packed LOD light task decrements with a true 3D BFS grid propagator.

---

### Last Updated: May 15, 2026*
