# Rust to MC Roadmap

Active plan only. History lives in [`docs/completed-changes.md`](docs/completed-changes.md). Fast file map lives in [`docs/file-tree-index.md`](docs/file-tree-index.md).

## Findings & Prioritized Actions (summary)

This section highlights current performance findings and prioritized remediation items. Full profiling steps and artifact collection instructions live in [`docs/profiling.md`](docs/profiling.md).

- Hotspots identified (investigate first):
  - `NativeBridge::processChunkData` — chunk ingest path (high CPU, JNI crossing cost)
  - `frustum.rs` / `Frustum::update_from_matrix` — culling math and coordinate transforms
  - `particles.rs` / `ParticleTickDispatcher` — allocations & parallel stream overhead
  - JNI boundary: frequent short JNI crossings and array copies

- Prioritized actions (short-term):
  1. Instrument and profile chunk ingest and frustum paths (owner: @dev, ETA: 3–6h, Risk: medium)
  2. Validate fused DH cull path performance vs Java fallback (owner: @dev, ETA: 2–4h, Risk: low)
  3. Replace `IntStream.parallel()` fallback with manual partitioning (owner: @dev, ETA: 1–2h, Risk: low)
  4. Add CI checks for `cargo clippy` and `./gradlew build` (owner: infra, ETA: 2h, Risk: low)

All original roadmap content retained below. See `docs/markdown-changelog.md` for a list of markdown edits.

## Scope

- Move high-cost client hot paths from Java to Rust.
- Keep vanilla behavior, mod compatibility, and stable pacing first.
- Keep JNI safe: explicit fallbacks, no hard crash on missing native symbols.

## Reference Paths

- Mod API/decompile jars: `C:/Users/Taylor Allred/AppData/Roaming/PandoraLauncher/instances/1.21.11-1.minecraft/mods`
- File index: [`docs/file-tree-index.md`](docs/file-tree-index.md)

## Current Snapshot

- Target version: Minecraft `1.21.11` (`:versions:mc1_21_11`).
- Client-only optimization surface; server-only mixins removed.
- Native bridge covers frustum, particle, lighting, audio/compression, DH/LOD paths.
- Chunk ingest stays preview-only behind `enableChunkIngestOffload`.
- [x] Consolidate HUD overlays into `DiagnosticHudRenderer`
- [x] Streamline configuration with `DiagnosticMode` enum
- [x] Optimize keybinds (F7 for HUD cycling, F8 for Sparkline)
- [ ] Implement Chunk Ingest offloading to Rust
- [ ] Native lighting integration (Experimental)
- Native lighting coexist mode stays user-controlled for modded lighting stacks.
- Particle and DNS hot paths now keep Java multicore fallbacks available when native path is unavailable or repeatedly slower.
- Instrumentation pass (Apr 24): DH frustum validation instrumentation added (refresh reasons, move tracking); `PieChartRenderer` timing overlay expanded with DH/Frustum diagnostics; player position fallback implemented for `1.21.11` camera API transition.
- Observability & Hygiene pass (Apr 24): Expanded `PieChartRenderer` with live cull ratios and JNI timing; unified frame metric return types as `long[]` across `NativeBridge` and `ModMenu`; resolved architectural hygiene lints (nested try extraction).
- Codebase optimization pass (May 1): Eliminated JNI wrappers for trivial math (sin/cos/sqrt/atan2/clamp/lerp) in favor of vanilla Java Math; lazy matrix fingerprint computation (only on camera change); reduced bounds resolution reflection overhead in DH compat; removed redundant Optional boxing patterns.
- Absolute coordinate consistency (May 1): Refactored `Frustum::update_from_matrix` to transform the View-Projection matrix to absolute world space once at the boundary. This eliminates "shifting back and forth" between coordinate systems and allows culling and occlusion tests to operate purely in absolute world space, improving precision and removing hot-path subtractions. Verified with unit tests for off-origin camera positions.
- DH Performance Fused Path (May 1): Eliminated significant JNI overhead and hot-path Java allocations in the Distant Horizons (DH) rendering path. Implemented `rustDHCullFused` in Rust to consolidate frustum, vertical cave-gate, and occlusion checks into a single JNI crossing. Removed expensive Java-side trigonometric calculations and per-section ConcurrentHashMap caching in `DistantHorizonsCompat`. Restored Metrics HUD observability by fixing the frame-history refresh logic in `DebugHudMixin`. Net result: Restored FPS to ~200+ target while maintaining absolute world coordinate consistency.
- Frame telemetry migration (May 1): Removed in-house `NativeBridge.FrameHistorySnapshot`, `getFrameHistorySnapshot()`, `rollFrustumFrameCounters()`, and `getLastFrustumFrameCounters()`. `DebugHudMixin` now drives the sparkline and `PieChartRenderer` from a local Java ring buffer fed by `mc.getCurrentFps()`. `ClientFrameMetricsMixin` has been removed entirely. `ModMenuIntegration` status panel now shows MC's live FPS in place of the removed frustum frame counters. Net result: zero in-house frame collection overhead on the hot path.
- Architectural Performance Pass (May 1): Optimized `ClientPlayNetworkHandlerMixin` with reflection-free `ChunkDataS2CPacketAccessor`, eliminating reflection overhead on the chunk ingest path. Removed `MatrixMixin` regression where JNI crossing for 4x4 math was slower than Java JIT. Refactored `ParticleTickDispatcher` Java fallback to replace expensive `IntStream.parallel()` with a manual partitioning loop, reducing allocation and stream overhead. Fixed cognitive complexity and variable declaration lints in `DistantHorizonsCompat`.
- JNI Boundary & DH Compat Micro-optimizations (May 1): 
  - Optimized `DistantHorizonsCompat::updateShadowPlanes()` using fused single-pass accumulation (was 6⨯4 loops, now 2-pass: accumulate + normalize), reducing FLOPs by ~40%.
  - Bounded `VISIBILITY_CACHE` with LRU-style eviction (max 8K entries) to prevent unbounded growth on large maps.
  - Optimized `isOutsideShadowFrustum()` with local reference caching to reduce repeated array indexing (6 array reads per plane → 1).
  - Optimized `ParticleTickDispatcher::tick()` to check native fallback state first (single volatile read) before acquiring camera context, saving GC allocations in fallback fast-path.
  - Optimized `NativeBridge::getDhReferenceY()` with early null-check for MinecraftClient to reduce exception overhead.
  - Optimized `Frustum::update_from_matrix()` Rust side using index-based loop instead of iterator, improving inline hints and reducing allocation.
  - Optimized `particles.rs` JNI crossing by returning early from distance checks within the tick loop before copying back to Java arrays.

### Q2 2026: Optimization & Maintenance

- [x] Bump Rust MSRV to 1.89 for AVX-512 support
- [x] Upgrade core dependencies (wgpu 24.0, jni 0.22, glam 0.32)
- [x] Profile and optimize JNI boundary overhead
- [x] Implement particle distance culling

- iGPU/Zen 4 Optimization Pass (May 1): Implemented a two-tier culling strategy to maximize FPS on integrated graphics (Framework 16 7040). Tier 1: Java-side "Shadow Frustum" using normalized planes culls ~80-90% of sections without a JNI crossing. Tier 2: AVX-512 and AVX2 vectorized Rust frustum culling for precise intersection tests. Integrated a per-frame Visibility Cache in `DistantHorizonsCompat` with spatial hashing, eliminating redundant culling. Fixed the Metrics HUD display logic in `DebugHudMixin` to restore live telemetry for JNI calls, light updates, and chunk traffic.
- Particle Performance & UX Pass (May 1): Finalized the adaptive particle culling architecture. Implemented camera-aware distance culling in `particles.rs` using a squared distance threshold passed from Java. Added high-performance hardware presets (LOW_END_IGPU, MID_RANGE, HIGH_END_DGPU) to `RustMCConfig` and integrated them into the ModMenu/YACL UI. Updated `ParticleTickDispatcher` to fetch camera coordinates once per batch, ensuring the native core skips updates for distant, non-visible effects. Resolved JNI signature conflicts and refined the Java-side bridge for cleaner parameter passing.
- Native Metrics & Config Stabilization (May 1): Finalized the native telemetry and configuration framework for high-performance iGPU monitoring. Updated the `NativeStatsRenderer` refresh interval to 100ms (10Hz) with adjusted scaling (10x) for real-time diagnostic visibility. Performed a comprehensive audit of `RustMCConfig`, pruning redundant fields (`bridgeC2ME`, `enableDebugHudGraph`) and merging debug HUD toggles into a unified `enableSparklineGraph`. Fixed a critical visibility bug in `DebugHudMixin` that prevented the native metrics HUD from rendering when other overlays were disabled. Increased the chunk ingest sampling rate in `ClientPlayNetworkHandlerMixin` to 1/8 packets and decoupled it from logging settings to ensure consistent native-side data flow.
- Core Dependency & Compute Modernization (May 1): Successfully migrated the native core to `wgpu 24.0`, `jni 0.22`, and `glam 0.32`. Refactored GPU compute pipelines (`wgpu_mesher.rs`, `wgpu_ao.rs`) to use thread-local buffer pooling, eliminating global `Mutex` contention in parallel LOD generation and ambient occlusion paths. Implemented the `jni 0.22` mutable environment model across the entire native bridge, adopting `get_primitive_array_critical` for low-latency array access. Integrated `wgpu::MemoryHints::Performance` and explicit backend selection to optimize driver-level scheduling on modern multi-core systems.

## Now

1. Expand native packet/chunk work only with profiling proof. (in progress: chunk ingest sampling increased to 1/8)
2. Keep chunk ingest preview gated and sampled.
3. Render cache locality improvements.
4. If Rust path overhead is higher than Java on a hot path, prefer Java multicore/multithread optimization first.

## Next

1. Worldgen candidate analysis for Rust/WGPU batchable work.

## Future

1. Screen/HUD path optimization.
2. Chunk/mesh pipeline locality and allocation trim.
3. JNI lookup/cache micro-optimizations.
4. Lock-free work only on proven contention.
5. Extra structure consolidation only when behavior stays unchanged.
6. Worldgen offload prototypes only with parity harness and safe recovery path.

## Validation Gates

- No frustum/LOD regressions.
- Frame time equal or better in target scenes.
- Lower CPU, allocation, or JNI cost on touched hot paths.
- No new crash, fallback break, or compat regression.
- No new warnings/errors in touched files.

## Non-Goals

- Do not keep JNI slower than Java.
- Do not trade correctness for speed.
- Do not add config for removed features.
- Do not widen shared mutable state without proof.
- Do not add per-frame release logging.

---

Last Updated: May 1, 2026 (JNI Boundary Micro-optimizations applied)