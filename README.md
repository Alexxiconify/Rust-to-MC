# Rust to MC
  
Optimizing Minecraft by offloading heavy computations to native Rust code using Java's Foreign Function & Memory (FFM) API.

## Features

- **Native Math**: Fast inverse square root and trig functions (Client/Server).
- **Efficient Networking**: Native Zlib compression/decompression (Client/Server).
- **Fast Generation**: SIMD-accelerated Simplex noise (Server/Internal).
- **Native Lighting**: (In Progress) Parallel light propagation (Server/Internal).
- **Optimized Pathfinding**: (In Progress) High-performance A* implementation (Server).

## Optimization Scope

### 💻 Client-Side Only

- **Frustum Culling**: Native AABB frustum tests for entities and blocks.
- **Occlusion Culling**: Hardware-accelerated occlusion queries hooked via Rust.
- **Rendering Compats**: Optimizations for Iris, Sodium, Distant Horizons, and EMF/ETF.
- **DNS Multi-Threading**: Parallelized server-list pinging and cached resolution.
- **Packet Pre-processing**: Background decryption/parsing for incoming chunk data.
- **Bootstrap Pre-warming**: Virtual-thread overlap for Mojang's DataFixerUpper initialization.

### 🌐 Hybrid / Server-Side

- **Native Compression**: Zlib offloading for packet payloads (Client out/Server in).
- **Pathfinding**: High-frequency mob pathing calculations offloaded to native A*.
- **Noise Generation**: World-gen noise samples (SIMD) used for chunk generation.
- **Lighting Engine**: Off-thread light propagation for massive block/sky light updates.
- **Command Parsing**: Native regex/logic for complex command execution.

## Architecture

The project consists of two main parts:

1. **Fabric Mod (Java)**: Handles Minecraft integration and Mixins.
2. **Rust Core (`rust_mc_core`)**: High-performance logic exposed via C-compatible FFI.

## Installation & Requirements

- **Java Version**: 21 or higher.
- **Fabric Loader**: 0.15.0 or higher.

## Compatibility

The mod includes a `MixinManager` that automatically disables specific optimizations if conflicting mods (like Lithium or Starlight) are detected.

---

## 🗺️ Future Plans

See our [ROADMAP.md](ROADMAP.md) for active optimization goals and planned work.

For finished work and historical notes, see [docs/completed-changes.md](docs/completed-changes.md).