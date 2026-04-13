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

## Completed Changes

Completed optimization and stabilization work is documented in [`docs/completed-changes.md`](docs/completed-changes.md).

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
- Minimize JNI overhead where it helps; avoid JNI where vanilla Java is faster.
- Document per-path strategy (copy vs pinned) for maintainability.
- Profile before/after every JNI change so regressions are easy to catch.

### 3) Config and Compat Cleanup

Goal: simplify the runtime surface without losing functionality.

- Keep `EntityRenderCompatMixin` as the single BBE/EMF/ETF/IF compat hook.
- Remove dead placeholder/accessor files when no longer referenced.
- Trim noisy inspections and stale suppressions without changing behavior.
- Prefer one clear toggle per optimization instead of overlapping config paths.

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
- Explore persistent buffer reuse in `FrustumMixin` to avoid per-frame allocations.
- Profile render-state lookups in `ParticleManagerMixin` and `RenderBudgetMixin` for cache thrashing.

### 6) Debug Visibility for Profiling (Next)

Goal: add internal observability hooks for performance validation.

- Expose frustum check counters and culling ratios via a debug HUD overlay.
- Add optional JNI call timing instrumentation (frame-time contribution per path).
- Log mixin exception fallbacks so silent degradation is visible in debug builds.
- Capture particle cull decisions and RenderState transitions for offline analysis.

### 7) Allocation Pressure Reduction (IN PROGRESS)

Goal: minimize GC overhead in hot render loops.

- **LightingMixin**: Profile PENDING_POS/PENDING_VAL ring buffer churn; consider pre-allocated double-buffering.
- **Screen Mixins**: Batch drawable updates instead of per-frame rebuilds; reduce string allocations in progress rendering.
- **MinecraftClientMixin**: Profile long[] array allocation in frame-time history; use circular index without allocation.

### 8) Screen & HUD Layer Optimization (Future)

Goal: optimize splash screen, loading screen, and debug overlays.

- **SplashOverlayMixin**: Profile gradient rendering and text measurement; validate GPU utilization during loads.
- **LevelLoadingScreenMixin**: Reduce per-frame resource reload progress polling.
- **WindowMixin**: Cache window size calculations; avoid reshape overhead on every frame.
- **DebugHudMixin**: Batch text rendering; profile memory format conversions for overlay data.
- **RenderBudgetMixin**: Implement lazy evaluation of FPS calculations; avoid redundant metric collection.

### 9) Chunk & Mesh Rendering Pipeline (Future)

Goal: optimize chunk building and vertex buffer management.

- **ChunkBuilderMixin**: Profile vertex data layout efficiency (interleaved vs separate buffers).
- Explore persistent memory-mapped chunks to reduce per-load allocation.
- Validate that chunk sort order doesn't regress under heavy mod loading.
- Profile vertex transformation cost in EMF/ETF scenarios; evaluate caching or lazy evaluation.

### 10) JNI Call Site Optimization (Future)

Goal: reduce JNI crossing overhead in high-frequency paths.

- Batch multiple Rust operations per JNI call where possible (e.g., frustum + cave check).
- Profile JNI method lookup cost vs direct native invocation.
- Consider JNI function pointers cache if repeated crossings dominate frame time.
- Validate that exception handling in fallback paths doesn't trigger costly class lookups.

### 11) Lock-Free Synchronization (Future)

Goal: replace synchronized blocks with lock-free alternatives in high-contention paths.

- **MinecraftClientMixin**: Evaluate AtomicLong for frame-time history instead of lock-based ring buffer.
- **LightingMixin**: Profile QUEUE_LOCK contention; consider ConcurrentLinkedQueue for work-stealing.
- **FrustumMixin**: Validate that matrix buffer reads are single-threaded or cache-friendly.
- Profile lock contention under heavy particle spawning or chunk loading.

## Validation Gates

Every optimization should satisfy at least one of these before it is considered ready:

- No visible correctness regression in frustum or LOD culling.
- Lower or unchanged frame time in the target scene.
- Lower CPU cost or allocation pressure on the measured hot path.
- No new crash, fallback breakage, or mod-compat regression.
- Clear benchmark or debug evidence that the change is doing useful work.

## Non-Goals / Guardrails

- Do not keep JNI hooks that are slower than vanilla Java paths.
- Do not ship optimizations that break frustum correctness or DH LOD visibility rules.
- Do not add config surface for features that are removed/defunct.

## Backlog

- Fast JSON bridge (`serde_json`) for selected resource paths, only if profiling shows repeated data-transform cost.
- ModMenu native stats surface (JNI calls, frustum checks, cache hits) if it helps diagnose culling or hot-path regressions.
- Additional benchmark scenes for heavy particles, DH, and networking spikes.

---

Last Updated: April 13, 2026