# Rust to MC Roadmap

This roadmap tracks the active plan for **Rust to MC**: move high-cost client hot-path work from Java to Rust while preserving gameplay correctness, mod compatibility, and stable frame pacing.

Scope: active work only. Completed optimization history lives in [`docs/completed-changes.md`](docs/completed-changes.md).

## Core Vision

- Keep vanilla behavior and visual correctness first, then optimize.
- Offload repeatable math/data-heavy work (frustum, particles, packet/data transforms) to Rust.
- Use JNI safely with predictable fallbacks so missing symbols never hard-crash gameplay.

## Current Baseline (April 2026)

- Primary target: **Minecraft 1.21.11** (active Gradle module `:versions:mc1_21_11`).
- Java side: **Client-only** with 10 optimized mixins focused on frustum, lighting, particles, and rendering.
- Mixin count reduced from 30+ to 20 active (7 pure server-side mixins removed for vanilla server compatibility).
- Thread usage optimized: virtual threads replaced with platform daemon threads for lower overhead.
- Java side emphasizes compatibility gating via `MixinManager` and `ModBridge`.
- Rust side provides frustum, particle, audio, compression, and utility paths with fallback wrappers in `NativeBridge`.
- Distant Horizons culling path remains Rust-driven with fused/fallback behavior.
- DH frustum fallback now stays visible until the first *confirmed native* matrix upload, preventing stale-pointer culling on world join.
- Native metrics are now wired end-to-end (`rustGetMetrics` + hot-path counters), so HUD/Mod Menu no longer report permanent zeros when JNI is active.
- Mod Menu + keybind coverage now includes the latest native math/debug and DH diagnostics toggles so runtime controls match config surface.
- Timing overlay is now text-only (no pie graphic), and keybind category translations are aligned so all Rust-MC binds are discoverable in Controls.
- Keybind category registration now uses a Controlling-safe namespace (`rustmc:keybinds`) so the category label resolves to `Rust-to-MC` consistently.
- DH frustum checks now tolerate both absolute and camera-relative section AABBs (absolute-first, camera-offset fallback) to prevent coordinate-space culling regressions.
- DH frustum compat now caches reflected method/field lookups, reuses a fixed VP matrix snapshot buffer, and learns preferred DH AABB coordinate-space mode to avoid repeated dual JNI cull calls.
- Rust particle ticking now reuses thread-local native scratch buffers and only enables Rayon for larger batches, reducing per-tick allocations and scheduling overhead.
- Particle spawn culling now caches squared cutoff distance at 20Hz, removing repeated per-spawn cutoff recomputation from `ParticleManagerMixin` hot paths.

## Completed Changes

Completed optimization and stabilization work is documented in [`docs/completed-changes.md`](docs/completed-changes.md).
- Rollback archive now lives in [`docs/rollback.md`](docs/rollback.md).

## Active Optimization Priorities

### 1) Frustum and DH Culling Reliability

Goal: keep culling correct, fast, and debuggable.

- Keep DH section visibility decisions in Rust.
- Validate fused culling behavior against the fallback path and edge camera/FOV cases.
- Add a debug toggle for culling visibility so testing can confirm what is being rejected.
- Capture section count, rejected count, and frame-time impact for each change.

### 2) JNI Hot-Path Hygiene

Goal: keep native offload only where it clearly wins.

- Keep wrappers safe and explicit on fallback behavior.
- Batch adjacent native work into fewer crossings when one call can cover multiple operations.
- Minimize JNI overhead where vanilla Java is faster.
- Document per-path strategy: copy vs pinned, single-call vs batched, and why.
- Profile before/after every JNI change.

### 3) Config and Compat Cleanup

Goal: simplify the runtime surface without losing functionality.

- Keep `EntityRenderCompatMixin` as the single BBE/EMF/ETF/IF compat hook.
- Remove dead placeholder/accessor files when no longer referenced.
- Trim noisy inspections and stale suppressions without changing behavior.
- Prefer one clear toggle per optimization instead of overlapping config paths.
- Avoid new sync points in compat glue unless they replace higher-cost paths.

### 4) Native Lighting, Packet, and Chunk Workloads

Goal: expand native offload only where profiling shows real payoff.

- Replace placeholder/bit-packed lighting propagation with a robust BFS-based Rust path.
- Validate against ScalableLux/Starlight ownership rules to avoid contention.
- Expand decoder/packet offload where allocation pressure is measurable.
- Continue DH/LOD mesh and data transform improvements only when world-load and flight scenarios stay stable.

### 5) Rendering Pipeline Cache Locality (Next)

Goal: improve CPU cache efficiency in hot render loops.

- Profile matrix buffer allocations in `MatrixMixin` for NUMA effects on multi-socket systems.
- Validate JOML matrix data layout (row-major vs column-major) against Rust SIMD expectation.
- Reuse persistent buffers in `FrustumMixin` instead of per-frame rebuilds.
- Profile render-state lookups in `ParticleManagerMixin` and `RenderBudgetMixin` for cache thrashing.
- Prefer contiguous, reused state over new per-frame objects.

### 6) Debug Visibility for Profiling (Next)

Goal: add internal observability hooks for performance validation.

- Expose frustum check counters and culling ratios via a debug HUD overlay.
- Add optional JNI call timing instrumentation with low overhead.
- Log mixin exception fallbacks so silent degradation is visible in debug builds.
- Capture particle cull decisions and `RenderState` transitions for offline analysis.
- Keep diagnostics opt-in and cheap when disabled.

### 7) Hot-Path Overhead Reduction (COMPLETE)

Completed work from this pass lives in [`docs/completed-changes.md`](docs/completed-changes.md) under `Lighting & JNI Hot-Path Trim`.

Remaining work:

- None right now. Next live optimization focus is section 8.

### 8) Screen & HUD Layer Optimization (Future)

Goal: optimize splash screen, loading screen, and debug overlays.

- **SplashOverlayMixin**: Profile gradient rendering and text measurement; validate GPU utilization during loads.
- **LevelLoadingScreenMixin**: Reduce per-frame resource reload progress polling.
- **WindowMixin**: Cache window size calculations; avoid reshape overhead on every frame.
- **DebugHudMixin**: Batch text rendering; minimize format conversions for overlay data.
- **RenderBudgetMixin**: Keep lazy FPS evaluation and shared metric caching.

### 9) Chunk & Mesh Rendering Pipeline (Future)

Goal: optimize chunk building and vertex buffer management.

- **ChunkBuilderMixin**: Profile vertex data layout efficiency (interleaved vs separate buffers).
- Explore persistent memory-mapped chunks to reduce per-load allocation.
- Validate that chunk sort order doesn't regress under heavy mod loading.
- Profile vertex transformation cost in EMF/ETF scenarios; evaluate caching or lazy evaluation.
- Keep mesh reuse safe across reloads and mod state changes.

### 10) JNI Call Site Optimization (Future)

Goal: reduce JNI crossing overhead in high-frequency paths.

- Batch multiple Rust operations per JNI call where possible (e.g., frustum + cave check).
- Profile JNI method lookup cost vs direct native invocation.
- Cache stable JNI function references only if repeated crossings dominate frame time.
- Validate that exception handling in fallback paths does not trigger costly class lookups.

### 11) Lock-Free Synchronization (Future)

Goal: replace synchronized blocks with lock-free alternatives in high-contention paths.

- **MinecraftClientMixin**: Keep frame-time history lock-free with a fixed buffer if it remains thread-safe.
- **LightingMixin**: Replace `QUEUE_LOCK` only if profiling proves the queue path dominates.
- **FrustumMixin**: Validate that matrix buffer reads stay single-threaded or cache-friendly.
- Profile lock contention under heavy particle spawning or chunk loading before broad refactors.

## Validation Gates

Every optimization should satisfy at least one of these before it is considered ready:

- No visible correctness regression in frustum or LOD culling.
- Lower or unchanged frame time in the target scene.
- Lower CPU cost or allocation pressure on the measured hot path.
- Lower lock contention on the measured path.
- Lower JNI crossing count or per-call overhead when native offload is involved.
- No new crash, fallback breakage, or mod-compat regression.
- Clear benchmark or debug evidence that the change is doing useful work.
- No new warnings or errors in touched files.

## Non-Goals / Guardrails

- Do not keep JNI hooks that are slower than vanilla Java paths.
- Do not ship optimizations that break frustum correctness or DH LOD visibility rules.
- Do not add config surface for features that are removed/defunct.
- Do not trade correctness for lock removal.
- Do not widen shared mutable state unless profiling proves the reuse pays off.
- Do not add per-frame logging in release paths.

## Backlog

- Fast JSON bridge (`serde_json`) for selected resource paths, only if profiling shows repeated data-transform cost.
- ModMenu native stats surface (JNI calls, frustum checks, cache hits) if it helps diagnose culling or hot-path regressions.
- Additional benchmark scenes for heavy particles, DH, and networking spikes.

---

Last Updated: April 14, 2026