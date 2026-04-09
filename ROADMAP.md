# Rust to MC Roadmap

This document outlines the future plans and feature goals for **Rust to MC**, a performance-focused Minecraft mod that offloads heavy computations to native Rust code.

## 🚀 Core Vision

To minimize Java's overhead in Minecraft's **client-side** hot-paths by leveraging Rust's safety, speed, and SIMD capabilities via the Foreign Function & Memory (FFM) API and JNI.

---

## 📅 Short-Term Goals (1-2 Months)

### 1. Parallel Lighting Propagation

- **Status**: Researching/In-Progress
- **Goal**: Fully implement the `rustPropagateLightBulk` hooks for Sodium and Starlight (client-side context).
- **Benefit**: Faster chunk updates and reduced stuttering when moving through differently lit areas.

### 2. WGPU Ambient Occlusion Stabilization

- **Status**: Stable (v22.1 Migration Complete)
- **Goal**: Maintain GPU-accelerated AO, now migrated to latest `wgpu` (22.x) and `sysinfo` (0.33) APIs.
- **Next Steps**:
  - **Voxel Grid Sampling**: Implement native voxel structure sampling in Rust for true geometric occlusion.
  - **Depth Buffer Occlusion**: Integrate with Iris depth buffers for screen-space AO effects.
- **Benefit**: Premium visual quality without the heavy CPU cost of traditional AO calculations.

---

## 🛠 Medium-Term Goals (3-6 Months)

### 1. Native Chunk Meshing

- **Status**: Planning
- **Goal**: Offload the construction of render meshes (Vertex Buffers) from Java to Rust.
- **Benefit**: Lower memory pressure (less GC) and faster chunk re-renders when blocks change.

### 2. Batched Entity & Particle Culling

- **Status**: Implemented / Optimizing
- **Goal**: Offload thousands of entity and particle intersection checks to Rust via batch JNI calls.
- **Benefit**: Eliminated per-object JNI overhead. Batch testing now uses Rayon for multi-threaded parallel execution.
- **Next Steps**: implement SIMD (SSE/AVX) for plane-point tests.

### 3. Native Client Packet Processing

- **Status**: Conceptual
- **Goal**: Use Rust to deserialize incoming networking packets directly into native structures, bypassing slow Java Reflection and reducing heap allocations.
- **Benefit**: Smoother experience on high-traffic servers and reduced GC pressure.

---

## 🌌 Long-Term Alpha Goals (6+ Months)

### 1. Full WGPU Renderer Backend

- **Goal**: A complete alternative to the OpenGL renderer using WGPU (WebGPU for Rust).
- **Benefit**: Modern API features (Vulkan/Metal/DX12), better multi-threading, and eliminated "OpenGL bottleneck."

---

## ✨ New Feature Ideas

- **Native DNS Dashboard**: An in-game UI to monitor and manage the DNS disk cache and resolution speeds.
- **Rust-MC Resource Monitor**: A premium HUD showing real-time Rust memory usage, SIMD status, and FFI transition latencies.
- **Ghost World API**: An API for other mods to query "Ghost Heights" and "Ghost Biomes" without loading chunks, perfect for world-map mods.
- **SIMD Audio Processor**: Real-time environmental audio effects (echo, occlusion) processed in parallel in Rust.

---

## 🤝 Compatibility Focus

We aim to stay compatible with the "Big 3" of Minecraft optimization:

- **Sodium**: Native meshing and lighting integration.
- **Lithium**: Offloading math and collection logic.
- **Iris**: Ensuring Rust-side rendering hooks don't break shader pipelines.

---

## ✅ Completed & Optimized

### 1. State Management & Lifecycle

- **Persistent Vanilla Frustums**: Implemented shared global context in Rust. Minecraft matrix updates are now synced once per frame via `FrustumMixin`, eliminating redundant object creation.
- **JNI Critical Access**: All high-count array transfers (Particles, Entities, Sound samples) now use `get_array_elements_critical` to bypass GC pauses and array copies.

### 2. API & Core Stability

- **Native Implementation Review**: Filled all missing native method gaps (Culling, Entity Batches) that were previously silent no-ops.
- **WGPU 22.1 Migration**: Successfully upgraded to the latest WebGPU API and `sysinfo` 0.33 with high-performance device descriptors.

---

## 🔍 Feature Backlog (From Source TODOs)

### 1. Advanced Light Engines (Rust-Side)

- **Starlight Depth-Aware Propagator**: Replace the current `*x -= 2` placeholder with true 17-block sky and block light propagation logic in Rust.
- **Distant Horizons BFS Lighting**: Replace the bit-packed decrement placeholder with a high-speed Breadth-First Search (BFS) on 3D grid data for optimized LOD lighting.

---

### Last Updated: April 9, 2026*
