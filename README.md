# Rust to MC

Optimizing Minecraft by offloading heavy computations to native Rust code using Java's Foreign Function & Memory (FFM) API.

## Features

- **Native Math**: Fast inverse square root and trig functions.
- **Efficient Networking**: Native Zlib compression/decompression.
- **Fast Generation**: SIMD-accelerated Simplex noise.
- **Native Lighting**: (In Progress) Parallel light propagation.
- **Optimized Pathfinding**: (In Progress) High-performance A\* implementation.

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

See our [ROADMAP.md](ROADMAP.md) for detailed future optimization goals and feature plans.
