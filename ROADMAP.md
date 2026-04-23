# Rust to MC Roadmap

Single source of truth for active direction and completed status. This file and [`docs/completed-changes.md`](docs/completed-changes.md) now carry the same fact set in different order.

## Scope

- Move high-cost client hot paths from Java to Rust without gameplay regressions.
- Keep vanilla behavior, mod compatibility, and stable frame pacing first.
- Keep JNI safe: explicit fallback paths, no hard crash on missing native symbols.

## Reference Paths

- Mod API/decompile reference jars: `C:/Users/Taylor Allred/AppData/Roaming/PandoraLauncher/instances/1.21.11-1.minecraft/mods`
- Rollback archive: [`docs/rollback.md`](docs/rollback.md)

## Current Snapshot (April 2026)

- Target version: Minecraft `1.21.11` (`:versions:mc1_21_11`).
- Architecture: client-only optimization surface; server-only mixins removed.
- Mixins: reduced from 30+ to 20 active; remaining set covers frustum, lighting, particles, rendering, client/hud/network/compat.
- Threading: virtual thread usage replaced by platform daemon threads on startup/preload/ping/prefetch paths.
- Native bridge: frustum, particle, lighting, audio/compression, DH/LOD utilities with guarded Java fallback wrappers.
- Chunk ingest migration: preview config gate added for Rust JNI chunk packet ingest groundwork (`enableChunkIngestOffload`), with safe symbol fallback caching.
- UI/config: timing overlay is text-only, keybind namespace is `rustmc:keybinds`, dead toggles removed, JNI metric status is explicit (`active`/`no-data`/`native-off`).

## Completed Highlights (Condensed Canonical Record)

### Frustum + DH Reliability

- April 18 pass: frustum point/AABB plane loops now use scalar plane coefficients and precomputed margin-expanded bounds to cut repeated hot-loop math.
- April 18 pass: DH frustum proxy now includes periodic rebind hardening so Rust-MC culling ownership stays active even if other compat layers attempt to replace bindings.
- DH culling remains Rust-driven with fused + fallback paths.
- Fallback visibility now holds until first confirmed native matrix upload (prevents stale-pointer join culls).
- DH culling space standardized to camera-minus; culling-space cycling controls removed.
- DH cave/frustum decisions are player-anchored (`client.player`) to avoid freecam/entity-camera drift.
- MiniHUD `RenderUtils` distance culling uses player camera source consistently.
- DH frustum refresh now keys off player state (position/rotation/FOV/aspect), not detached freecam matrix changes.
- Optional DH JNI symbols (margin/vertical/fused/occlusion) are cached once, avoiding repeated exception-path probes.
- DH API method-shape diagnostics are logged once at registration with capped unresolved notices.
- DH JNI update/relight/LOD-mesh handoffs use defensive array snapshots.
- DH surface gate tuned to `54` for lower swim-near-ocean over-cull risk.
- DH occlusion is frustum-first; only frustum-kept chunks can occlude; camera-relative fallback skips vertical cave gate.
- `rustOcclusionTest(...)` JNI export added for DH occlusion parity.

### JNI + Hot-Path + Rendering

- Native metrics are wired end-to-end (`rustGetMetrics` + counters) for HUD/Mod Menu.
- April 18 pass: single-AABB frustum JNI test now uses coordinate-based checks directly (no temporary mins/maxs arrays), and batch frustum path now uses one precomputed `count_usize` sizing path.
- Frustum fallback wrappers were deduplicated and optimized (single captured context, shared all-visible fallback path).
- Rust frustum AABB hot loop removed temporary min/max array creation via scalar helper path.
- Lighting offload hot path trimmed (queue mask/snapshot strategy, index-math hoists, less busy-spin/Rayon overhead).
- Render/HUD trims: shared telemetry snapshot, reduced atomics, cached render-budget tier lookups, fewer repeat JNI pulls.
- Particle paths: native scratch buffer reuse, Rayon only for larger batches, spawn cutoff squared-distance cache at 20Hz.
- April 23 pass: particle cutoff scaling fixed so low-FPS/heavy-mod modes tighten distance as intended (reduced particle load under stress).
- LOD mesh/GPU paths skip empty inputs early in Java and Rust.

### Build + Packaging + Networking

- Gradle/Cargo integration now tracks Rust inputs precisely and stages native output in `build/generated/rust-resources`.
- `sourcesJar` no longer triggers Rust binary staging work.
- `:versions:mc1_21_11` removed local Xaero jar dependency assumptions.
- DNS cache persistence now includes join/disconnect in addition to unload/exit.
- April 23 pass: startup config load no longer rewrites valid unchanged `rust-mc.json`, reducing unnecessary disk I/O during load-in.
- April 23 pass: preview chunk ingest now hooks client chunk receive path (`ClientPlayNetworkHandler` mixin, `require=0`) and forwards lightweight chunk metadata to Rust JNI.
- April 23 pass: native metrics expanded with chunk ingest packet/byte counters and surfaced in Mod Menu status.

### Java Structure Consolidation (Section 12)

- Pass #1 complete: JNI math fallback wrappers + DH readiness guards centralized.
- Pass #2 complete: DNS cache + connection glue unified through `DnsCacheUtil`; stale per-math toggles removed.
- Follow-up complete: DNS mixins moved under `mixin.network`; stale manager entries/comments pruned.
- Passes #3-#7 complete: mixin package flattening/reclassification (`mixin.performance`, `mixin.client`, `mixin.integration`) and HUD mixin merge cleanup.
- Pass #8 complete: ELB classes moved from `com.iafenvoy.elb.*` to `com.alexxiconify.rustmc`.

### Compatibility and Cleanup

- April 18 pass: DH occlusion `submit/test` now cache matrix/camera components per call to reduce repeated slice indexing in per-box projection math.
- April 18 pass: DH occlusion culling now only returns culled when all sampled points (corners + center) are hidden, preventing partial-visibility false culls.
- Seven server-only mixins removed; client-only compatibility posture is now explicit.
- `EntityRenderCompatMixin` remains the unified BBE/EMF/ETF/IF compat hook.
- Mod detection caching (`ModBridgeCache`) added to avoid repeated hot-path checks.
- Removed dripstone culling plumbing from config/UI; vanilla frustum update only emits cave-status signal.

### Chunk/Worldgen GPU Migration Track

- April 23 kickoff: `NativeBridge.processChunkData(...)` now hard-gated by config and native symbol availability cache to avoid repeated link-error paths.
- April 23 kickoff: Rust JNI now exports `rustProcessChunkData(...)` and `rustRequestMemoryCleanup(...)` with safe no-crash behavior and chunk ingest counters.
- Current phase: ingest and instrumentation only; no gameplay-critical decode replacement yet.
- Next phase gate: add client chunk receive hook behind `require=0` mixin + correctness parity checks before enabling by default.
- Worldgen note: move noise/sample experimentation only after chunk ingest parity and profiling evidence.

## Active Priorities (Now -> Next -> Future)

### Now

1. Frustum and DH culling reliability validation on edge camera/FOV/world-join cases.
2. JNI crossing hygiene: batch where beneficial, keep Java fallback where faster.
3. Config and compat cleanup: remove dead accessors/placeholders and stale suppressions.
4. Native lighting/packet/chunk expansion only where profiling shows measurable win.
5. Chunk ingest offload rollout (preview-only): compare chunk correctness and verify frame pacing impact with receive hook active.

### Next

1. Rendering cache locality (`MatrixMixin`, render-state lookups, JOML layout validation).
2. Debug observability (frustum counters, cull ratios, optional low-overhead JNI timing).
3. Worldgen candidate analysis: identify CPU-heavy client-accessible sampling paths suitable for Rust/WGPU batching.

### Future

1. Screen/HUD path optimization (`SplashOverlayMixin`, `LevelLoadingScreenMixin`, `WindowMixin`).
2. Chunk/mesh pipeline locality and allocation reduction.
3. JNI call-site lookup/caching micro-optimizations.
4. Lock-free synchronization work only where profiling proves contention.
5. Additional Java structure consolidation where behavior remains unchanged.
6. Backlog: selective `serde_json` bridge, expanded benchmark scenes, optional deeper Mod Menu stats.
7. Worldgen offload prototypes (noise/height/biome batch jobs) only with parity harness and rollback guard.

## Validation Gates

Ship only when one or more gates are met without regressions:

- No visible frustum/LOD correctness regressions.
- Frame time lower or unchanged in target scenes.
- CPU cost/allocation pressure reduced on measured hot path.
- Lock contention reduced where touched.
- JNI crossing count or per-call overhead reduced where native offload is used.
- No new crash, fallback break, or compat regression.
- Debug/benchmark evidence confirms impact.
- No new warnings/errors in touched files.

## Non-Goals / Guardrails

- Do not keep JNI paths slower than equivalent Java path.
- Do not trade culling correctness for speed.
- Do not add config surface for removed/defunct features.
- Do not remove locks without contention proof.
- Do not widen shared mutable state without measured payoff.
- Do not add per-frame release logging.

---

Last Updated: April 23, 2026