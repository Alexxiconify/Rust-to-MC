# Rust to MC Roadmap

Performance-focused Minecraft mod that offloads hot-paths to native Rust via JNI/FFM.

**Goal**: Minimize Java GC pressure and CPU overhead on the **client** render/tick threads using Rust SIMD, zero-copy JNI, and parallel algorithms.

---

## ✅ Completed

### Frustum Culling

- **High-Precision f64 Frustum**: All plane extraction and AABB intersection math migrated to `f64`. Fixes North/East LOD unloading/culling errors far from spawn by eliminating `f32` precision loss.
- **SIMD Point Test (SSE2 fallback)**: 6 real planes tested in `f64`.
- **Adaptive FOV Scale**: `fov_scale` baked into margin; normalizes AABB bounds in native code to prevent aggressive culling.
- **Shared Global Frustum**: Persistent native context synced once per frame; no per-call frustum recreation.

### Lighting

- **True 3D BFS (Starlight)**: Replaced 1D-decrement stub with a full neighbor-propagating BFS on a thread-local 34³ grid; fans out across all 6 faces per light source.
- **Zero-Alloc Lighting Queue**: `ArrayBlockingQueue<int[]>` replaced with primitive `long[]` ring buffer.
- **Atomic Lighting Queue**: `synchronized` enqueue replaced with lock-free `AtomicInteger` head/tail; virtual-thread drain thread unchanged.
- **Parallel DH Lighting**: `rustPropagateLightDH` uses Rayon par_iter over packed `long[]` tasks.
- **Zero-Copy Lighting**: Critical pinning (`NoCopyBack`/`CopyBack`) for `propagateLightBulk` and `propagateLightDH`.

### Rendering & Math

- **SIMD Matrix Math**: All `Matrix4f.mul` zero-copy, column-major SIMD-friendly layout.
- **Native HUD Matrix Stack**: Chain multiply in Rust; minimizes JNI roundtrips for deep model hierarchies.
- **AVX2 Vertex Transform**: `rustTransformVertices` inner loop replaced with AVX2 8-wide FMA — computes vertex + normal in a single 8-lane pass; 2× throughput on batches >512 (Rayon parallel). SSE2 scalar fallback on pre-Haswell.
- **Hardware InvSqrt (RSQRTSS)**: One SSE instruction + Newton-Raphson; replaces magic-number approximation.
- **Trig LUT (65536 entries)**: Pre-warmed on background thread during `JNI_OnLoad`; matches Java `MathHelper` exactly.
- **Fast atan2**: Polynomial approximation; avoids libc call overhead.
- **Adaptive Particle Culling**: Throttle relaxes with ImmediatelyFast, tightens with EMF/ETF. Migrated to `tick()` for 1.21.11.
- **Parallel Map Texture**: `rustProcessMapTexture` / `rustProcessMapTexturePtr` parallelize Item Frame color math.
- **SIMD AO**: WGPU-backed ambient occlusion compute via `rustComputeAmbientOcclusion`.

### PRNG & Noise

- **Xoshiro256++ PRNG**: Native random offloads `Xoroshiro128PlusPlusRandom` and `LocalRandom`.
- **Zero-Contention PRNG**: Global `Mutex<[u64;4]>` → `thread_local! RefCell`; zero lock per call.
- **Atomic PRNG Seed Broadcast**: `rustRandomSetSeed` stores to `GLOBAL_PRNG_SEED` + increments `GLOBAL_PRNG_VERSION`; every thread reseeds lazily on next PRNG call — true cross-thread consistency, zero per-call cost on hot-path.
- **Simplex Noise**: Thread-local Simplex auto-reseeds on global seed change; safe for Rayon workers.

### Networking & Compression

- **Packet Filter**: `rustProcessPacket` / `rustProcessPacketDirect` now filter KeepAlive (`0x24`), Ping (`0x1F`), Bundle Delimiter (`0x00`), Entity Velocity (`0x51`), Update Time (`0x5C`).
- **Zero-Alloc Inflate**: `rustInflateRaw` — allocation-free world chunk decompression.
- **Rust Zlib Compress**: `rustCompress` with adaptive level (fast for large, level-1 for small).
- **DNS Cache**: In-memory + disk-persistent DNS resolver with background refresh; batch parallel resolve via Rayon.

### Audio

- **SIMD Audio Suite**: Volume + stereo pan via Rayon `par_chunks_mut(2)`; handles mono fallback.
- **Sound Physics + Cave Reverb**: Distance attenuation and occlusion dampening. When `in_cave=true`, runs an 8-tap FIR convolution reverb kernel (ITD: 4–512 samples, decaying weights); zero allocation.

### Infrastructure

- **Thin LTO + Parallel Codegen**: Release build uses Thin LTO and disabled incremental to prevent cache corruption.
- **LLD Linker**: `rust-lld` on Windows MSVC; drastically reduces link times.
- **Persistent Lib Cache**: Native binary cached in config dir; skips extraction on subsequent launches.
- **Virtual Threaded Init**: DH + ScalableLux compat layers initialize in parallel on virtual threads.
- **Ultra-Fast Startup**: WGPU and DNS cache load on background threads; no blocking joins in `onInitialize`.
- **GPU-Accelerated LOD Generation**: Compute-shader based voxelization and meshing (`lod_mesher.wgsl`) via WGPU. Offloads high-detail DH LOD construction to the GPU, significantly reducing builder thread CPU load.
- **Maximized DH LOD Performance**: Boosted Distant Horizons threading (LOD Builder, World-Gen, File-Save) via reflection; set LOD Builder to HIGH priority via `setLodBuilderPriority(0)`. Optimizes processing of server-sourced LOD data.
- **Chunk Builder Expansion**: `ChunkBuilderMixin` uses `max(2, cpus - 2)` workers; yields to Sodium.
- **ModMenu Stats HUD**: Atomic counters expose JNI call volume, lighting updates, frustum tests in-game.
- **1.21.11 Mixin Remaps**: All 5 `Cannot remap` errors fixed against yarn build.4 mappings.
- **Zero Warnings**: All major Clippy and Java IDE warnings resolved.
- **Fast JSON (`rustLoadJson`)**: `serde_json` parses + minifies JSON in Rust; returned as Java String. Replaces GSON for resource/language file parsing; no GC allocation on hot language-load path.

---

## 🚧 In Progress

### Native Chunk Meshing & Parsing

- **Status**: Stub — `rustProcessChunkData` parses root compound tag header only.
- **Goal**: Full section-level block-state decoder (palette + packed-long data) via direct ByteBuffer pointer; then vertex buffer construction bypassing Java NBT.
- **Next**: Hook `ClientChunkMap` or `ChunkSerializer` to pass raw section bytes via `long ptr`; implement palette decoder in Rust.

---

## 🔮 Upcoming Optimizations

### High Priority

### Medium Priority

1. **NBT Serialization Offload** — `rustNbtDecodeInt` extended to decode full compound trees for high-traffic network packets.
2. **Distant Horizons 3D BFS** — true 3D LOD light propagation grid for DH (currently only 1D `long[]` decrement).

---

### Last Updated: April 12, 2026

**Distant Horizons & Performance Overhaul:**

- **Modular Core Architecture**: Refactored `lib.rs` into specialized submodules (`frustum`, `lighting`, `math`, `net`, `particles`, `pipeline`) for better maintainability and performance.
- **GPU-Accelerated LOD Generation**: Compute-shader mesh voxelization (`lod_mesher.wgsl`) via WGPU, reducing builder thread CPU pressure.
- **Streamlined DH Pipeline**: Unified `LodPipeline` handling "Generate -> Shade (AO) -> Render" entirely in Rust.
- **f64 High-Precision Frustum**: Migrated planes to `f64` world-coordinates, eliminating LOD unloading at north/east distance.
- **Fused JNI Culling**: Halved JNI overhead by combining subterranean and frustum checks into one native call.
- **Aggressive DH Threading**: Optimized thread pools and builder priority via reflection for peak server-sourced LOD throughput.
- **SIMD Refines**: Fixed borrow-checker and syntax lints across the native bridge.
