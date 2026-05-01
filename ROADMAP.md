# Rust to MC Roadmap

Active plan only. History lives in [`docs/completed-changes.md`](docs/completed-changes.md). Fast file map lives in [`docs/file-tree-index.md`](docs/file-tree-index.md).

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
- UI/debug overlays now focus on Debug HUD Frame Graph + Timing Info overlay (F6), with JNI metrics in Mod Menu status.
- Native lighting coexist mode stays user-controlled for modded lighting stacks.
- Particle and DNS hot paths now keep Java multicore fallbacks available when native path is unavailable or repeatedly slower.
- Instrumentation pass (Apr 24): DH frustum validation instrumentation added (refresh reasons, move tracking); `PieChartRenderer` timing overlay expanded with DH/Frustum diagnostics; player position fallback implemented for `1.21.11` camera API transition.
- Observability & Hygiene pass (Apr 24): Expanded `PieChartRenderer` with live cull ratios and JNI timing; unified frame metric return types as `long[]` across `NativeBridge` and `ModMenu`; resolved architectural hygiene lints (nested try extraction).
- Codebase optimization pass (May 1): Eliminated JNI wrappers for trivial math (sin/cos/sqrt/atan2/clamp/lerp) in favor of vanilla Java Math; lazy matrix fingerprint computation (only on camera change); reduced bounds resolution reflection overhead in DH compat; removed redundant Optional boxing patterns.
- Absolute coordinate consistency (May 1): Refactored `Frustum::update_from_matrix` to transform the View-Projection matrix to absolute world space once at the boundary. This eliminates "shifting back and forth" between coordinate systems and allows culling and occlusion tests to operate purely in absolute world space, improving precision and removing hot-path subtractions. Verified with unit tests for off-origin camera positions.
- DH Performance Fused Path (May 1): Eliminated significant JNI overhead and hot-path Java allocations in the Distant Horizons (DH) rendering path. Implemented `rustDHCullFused` in Rust to consolidate frustum, vertical cave-gate, and occlusion checks into a single JNI crossing. Removed expensive Java-side trigonometric calculations and per-section ConcurrentHashMap caching in `DistantHorizonsCompat`. Restored Metrics HUD observability by fixing the frame-history refresh logic in `DebugHudMixin`. Net result: Restored FPS to ~200+ target while maintaining absolute world coordinate consistency.
- Frame telemetry migration (May 1): Removed in-house `NativeBridge.FrameHistorySnapshot`, `getFrameHistorySnapshot()`, `rollFrustumFrameCounters()`, and `getLastFrustumFrameCounters()`. `DebugHudMixin` now drives the sparkline and `PieChartRenderer` from a local Java ring buffer fed by `mc.getCurrentFps()`. `ClientFrameMetricsMixin` has been removed entirely. `ModMenuIntegration` status panel now shows MC's live FPS in place of the removed frustum frame counters. Net result: zero in-house frame collection overhead on the hot path.
- Architectural Performance Pass (May 1): Optimized `ClientPlayNetworkHandlerMixin` with reflection-free `ChunkDataS2CPacketAccessor`, eliminating reflection overhead on the chunk ingest path. Removed `MatrixMixin` regression where JNI crossing for 4x4 math was slower than Java JIT. Refactored `ParticleTickDispatcher` Java fallback to replace expensive `IntStream.parallel()` with a manual partitioning loop, reducing allocation and stream overhead. Fixed cognitive complexity and variable declaration lints in `DistantHorizonsCompat`.

## Now

1. Expand native packet/chunk work only with profiling proof. (in progress: chunk ingest stats tracking active)
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

Last Updated: May 1, 2026
