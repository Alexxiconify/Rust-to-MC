# Completed Changes

Historical record. Active plan lives in [`ROADMAP.md`](../ROADMAP.md); fast file lookup lives in [`docs/file-tree-index.md`](file-tree-index.md).

## Reference Paths

- Mod API/decompile reference jars: `C:/Users/Taylor Allred/AppData/Roaming/PandoraLauncher/instances/1.21.11-1.minecraft/mods`

## Current Snapshot (April 2026)

- Target version: Minecraft `1.21.11` (`:versions:mc1_21_11`).
- Architecture: client-only optimization surface; server-only mixins removed.
- Mixins: reduced from 30+ to 20 active.
- Threading: platform daemon threads on startup/preload/ping/prefetch in place of virtual-thread usage.
- Native bridge: guarded JNI offload with explicit Java fallbacks.

## Chronological Milestones

### April 23, 2026

#### Chunk/Worldgen GPU Migration Kickoff (Preview Foundation)

- `src/main/java/com/alexxiconify/rustmc/config/RustMCConfig.java` config version bumped to `2.7.0`; added `enableChunkIngestOffload` (default `false`) and `enableChunkIngestValidation` (default `false`) for controlled rollout and throttled preview telemetry.
- `src/main/java/com/alexxiconify/rustmc/config/ModMenuIntegration.java` now exposes `Chunk Ingest Offload (Preview)` toggle under native features.
- `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` `processChunkData(...)` now uses config/symbol gates with one-way `UnsatisfiedLinkError` fallback caching to avoid repeated hot-path exception checks.
- `src/main/java/com/alexxiconify/rustmc/mixin/network/ClientPlayNetworkHandlerMixin.java` adds preview `require=0` chunk receive hook to forward chunk packet payload snapshots into JNI ingest path.
- `src/main/java/com/alexxiconify/rustmc/mixin/network/ClientPlayNetworkHandlerMixin.java` now snapshots real `ChunkDataS2CPacket` bytes for JNI handoff and emits throttled validation logs (5s interval) when enabled.
- FPS recovery pass: `src/main/java/com/alexxiconify/rustmc/mixin/network/ClientPlayNetworkHandlerMixin.java` now sends lightweight coord payload by default and samples full packet snapshots only during validation windows (1/32 packets).
- FPS recovery pass: `ClientPlayNetworkHandlerMixin` payload reflection lookups are resolved once and reused, removing per-packet method-table scans.
- `rust_mc_core/src/lib.rs` now exports `rustProcessChunkData(...)` and `rustRequestMemoryCleanup(...)` JNI symbols with safe no-crash behavior and ingest counters.
- FPS recovery pass: `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` now tracks ingest `System.nanoTime` only when validation is enabled, cutting per-packet timing overhead.
- FPS recovery pass: `rust_mc_core/src/lib.rs` chunk ingest now trusts validated Java length and skips redundant JNI `get_array_length` checks.
- FPS recovery pass: `src/main/java/com/alexxiconify/rustmc/mixin/network/ClientPlayNetworkHandlerMixin.java` now hard-skips chunk ingest JNI/allocation work unless validation sampling is active, removing steady-state packet overhead in preview mode.
- `src/main/java/com/alexxiconify/rustmc/compat/DistantHorizonsCompat.java` now restores below-`54` DH cave gating in camera-minus mode by short-circuiting DH visibility when player Y is under the surface gate and `enableDhCaveCulling` is enabled.
- `src/main/java/com/alexxiconify/rustmc/mixin/network/ClientPlayNetworkHandlerMixin.java` validation logging helper now removes constant-true flags from call signatures to clear IDE warnings.
- `src/main/java/com/alexxiconify/rustmc/config/ModMenuIntegration.java` status panel now shows chunk ingest packets/bytes from native metrics plus Java-side attempts/failures/avg JNI microseconds.
- Track status: ingest/instrumentation only; no gameplay-critical chunk decode/worldgen replacement enabled yet.

### April 24, 2026

#### Single-Pass Roadmap Sweep (Frustum/DH + JNI + Compat Noise)

- `src/main/java/com/alexxiconify/rustmc/MixinManager.java` mixin condition ownership semantics corrected so `MatrixMixin` and `LightingMixin` only apply when Rust-MC owns the path.
- `src/main/java/com/alexxiconify/rustmc/compat/DistantHorizonsCompat.java` DH frustum proxy now handles null/variant intersect arg shapes (array payloads, object getter forms) more defensively and keeps conservative visible fallback on unresolved shapes.
- `src/main/java/com/alexxiconify/rustmc/compat/DistantHorizonsCompat.java` matrix extraction path refactored into helper methods to reduce complexity warnings while preserving legacy/new DH API support.
- `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` batch frustum JNI wrappers now share one fallback path and reuse a shared empty-byte constant to trim wrapper churn.
- `src/main/java/com/alexxiconify/rustmc/ModBridge.java` removed large dead commented compat stub block/suppression noise, retaining only actively used mod flags and ownership predicates.
- Validation: `./gradlew.bat :versions:mc1_21_11:build` passed.

#### JNI Stats Snapshot Reuse + HUD Render Churn Trim

- `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` now reuses per-thread snapshot buffers for frustum counters, chunk ingest stats, and metrics reads, and swaps frame-history cache churn from atomic snapshot plumbing to a lighter volatile cache.
- `src/main/java/com/alexxiconify/rustmc/util/NativeStatsRenderer.java` now respects the 5-field metrics snapshot while rendering only the 3 HUD lines it needs.
- `src/main/java/com/alexxiconify/rustmc/util/PieChartRenderer.java` no longer allocates a per-frame line array for the timing overlay.
- `src/main/java/com/alexxiconify/rustmc/RustMCClient.java` native-stats keybind now flips `enableNativeMetricsHud` and `enablePieChart` together.
- `src/main/java/com/alexxiconify/rustmc/config/ModMenuIntegration.java` native-stats checkboxes now share one setter, so HUD and pie-chart tracker stay synchronized.
- `src/main/java/com/alexxiconify/rustmc/RustMC.java` now fingerprints build output and resets stale config when jar contents change without a config version bump.
- `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` cache extraction now hashes bundled DLL bytes and deletes stale `rustmc-bin` entries before loading a replacement.
- `rust_mc_core/src/wgpu_ao.rs` now caps retained GPU buffers harder and exposes a cleanup trim path that runs from JNI memory cleanup.
- `rust_mc_core/src/wgpu_mesher.rs` now trims retained GPU buffers harder after spikes, and `rust_mc_core/src/lib.rs` calls the new pool trim during JNI memory cleanup.
- `rust_mc_core/src/occlusion.rs` lowered heuristic depth-buffer resolution to reduce resident memory.

#### Startup Load-In + Particle Pressure Trim

- `src/main/java/com/alexxiconify/rustmc/RustMC.java` startup config path now skips unconditional rewrite when config schema is already current, reducing load-in disk churn.
- `src/main/java/com/alexxiconify/rustmc/RustMC.java` startup/internal config writes now avoid unnecessary `ElbConfig` persistence; explicit user saves still persist ELB settings.
- `src/main/java/com/alexxiconify/rustmc/mixin/ParticleManagerMixin.java` cutoff scaling corrected (`* 0.4`, `* 0.6`) so low-FPS/heavy-mod states cull more aggressively instead of expanding particle distance.

#### Overlay Surface Cleanup + Lighting Coexist Accuracy

- `src/main/java/com/alexxiconify/rustmc/config/RustMCConfig.java` now treats legacy `enableNativeMetricsHud` as an alias for `enablePieChart` so old configs migrate to the working timing overlay path; config schema bumped to `2.7.1`.
- `src/main/java/com/alexxiconify/rustmc/RustMCClient.java` F6 toggle now directly controls timing overlay state and action-bar/log text now reflects timing overlay behavior.
- `src/main/java/com/alexxiconify/rustmc/config/ModMenuIntegration.java` removed dead Native Metrics HUD toggle, added lighting status/readiness text, exposed `experimentalCoexistEnabled`, and updated lighting descriptions to match coexist behavior.
- `src/main/java/com/alexxiconify/rustmc/ModBridge.java` + `src/main/java/com/alexxiconify/rustmc/mixin/performance/LightingMixin.java` now gate native lighting on explicit mod-ownership rules so coexist ON keeps Rust lighting active while coexist OFF yields to intrusive lighting owners.
- `src/main/java/com/alexxiconify/rustmc/ModBridge.java` ownership predicates were corrected so bridge semantics match labels (`isMathOwned`, `isNetworkingOwned`, `isFrustumOwned`) and native hooks no longer get disabled by inverted checks.
- `src/main/resources/assets/rust-mc/lang/en_us.json` F6 translation now reads "Toggle Timing Info Overlay".

#### Java Multicore Fallback Expansion (Hot-Path Safety Net)

- `src/main/java/com/alexxiconify/rustmc/util/ParticleTickDispatcher.java` now drives adaptive particle ticking with Java-parallel fallback (`IntStream.parallel`) and auto-switches after repeated slow native batches; `NativeBridge.tickParticlesAdaptive(...)` routes compat callers through this dispatcher.
- `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` `dnsBatchResolve(...)` now falls back to Java DNS resolution with parallel batching when native DNS symbols are unavailable.
- `src/main/java/com/alexxiconify/rustmc/NativeBridge.java` pathfinding scratch arrays moved to `ThreadLocal` storage to keep zero-allocation behavior while remaining thread-safe under modded call patterns.
- `src/main/java/com/alexxiconify/rustmc/compat/ParticleRainCompat.java` now always routes through `NativeBridge.tickParticlesAdaptive(...)`, allowing native or Java fallback execution without silently disabling optimization when native load fails.

### April 18, 2026

#### Rust DH Frustum + Occlusion Micro-Optimization Pass

- `rust_mc_core/src/frustum.rs` now uses scalar plane coefficients in frustum point/AABB loops and precomputes margin-expanded AABB bounds once per test.
- `rust_mc_core/src/lib.rs` frustum JNI single-AABB path now calls `is_outside_aabb_coords(...)` directly (no temporary mins/maxs arrays), and batch path now reuses one `count_usize` sizing path.
- `rust_mc_core/src/occlusion.rs` `submit(...)` and `test(...)` now cache matrix/camera components per call to reduce repeated indexing in projection math.
- `rust_mc_core/src/occlusion.rs` now culls DH chunks only when all sampled points (8 corners + center) are hidden, preventing partial-visibility false culls.
- `src/main/java/com/alexxiconify/rustmc/compat/DistantHorizonsCompat.java` now rebinds the Rust frustum culler periodically to keep Rust-MC culling authority when other hooks attempt replacement.

### April 14, 2026

#### Background Thread Sweep

- `RustMC.java`, `PreLaunchHandler.java`, `ServerPingerMixin.java`, and `DistantHorizonsCompat.java` background work moved to platform daemon threads.

#### Frustum AABB Hot-Path Trim (Java + Rust)

- `NativeBridge.updateVanillaFrustumAndCave()` fallback reuses one captured context.
- `rust_mc_core/src/frustum.rs` added scalar `is_outside_aabb_coords(...)` helper.
- `rust_mc_core/src/lib.rs` batch frustum loop switched to scalar helper to avoid temporary arrays.

#### Build-Speed Pass (Gradle + Cargo)

- Rust input tracking tightened in `build.gradle`.
- Cargo invocation simplified to direct `cargo build --release --locked`.
- Native outputs staged under `build/generated/rust-resources`.
- `ProcessResources` consumes staged assets.
- `sourcesJar` decoupled from Rust binary staging.

#### Lighting Hot-Path Micro-Trim

- `rust_mc_core/src/lighting.rs` writeback loop now reuses precomputed stride (`plane`).

#### Frustum Compat Fallback Fix

- `NativeBridge.invokeFrustumIntersect(...)` now executes real native frustum test (no dead `-1` stub).

#### DH Test Path + Config Refresh

- Dead pie-chart keybind removed.
- Semantic config versioning (`2.x.y`) enforced for clean refresh behavior.
- DH culling/testing set to player position only.
- DH surface gate tuned to `54`.
- Mod Menu options aligned to current DH behavior.

#### DH LOD Occlusion Ordering + DNS Persist Hooks

- DH occlusion now runs after frustum keep decisions.
- Only frustum-kept LOD chunks are used as occluders.
- Camera-relative fallback skips vertical cave gate.
- `rustOcclusionTest(...)` JNI exported for DH parity.
- DNS persistence now includes join/disconnect.

#### Java Consolidation Pass #1

- `NativeBridge` JNI math fallback logic centralized.
- DH readiness checks centralized in `DistantHorizonsCompat`.

### April 13, 2026

#### Client-Only Mixin Refactor + Hot-Path Cleanup

- Frame telemetry and render-budget reads consolidated into shared snapshots/cached tiers.
- `ParticleManagerMixin` and HUD paths reduced repeated lookups and per-frame overhead.
- Lighting worker and Rust lighting writeback path trimmed to reduce spin/scheduling overhead.
- `NativeBridge.batchFrustumTest()` moved to shared all-visible fallback helper.
- Empty-input fast paths added for LOD mesh/GPU work on Java and Rust sides.

#### Mixin Surface Cleanup

- Server-only mixins removed (`RandomMixin`, `CommandManagerMixin`, `PathfindingMixin`, `DecoderHandlerMixin`, `PacketDeflaterMixin`, `SimplexNoiseSamplerMixin`, `SchemasMixin`, `BlockStateMixin`).
- Mixin set consolidated to 20 active client-first mixins.

#### Thread and Compat Infrastructure

- Virtual-thread paths replaced with platform daemon thread usage in key background flows.
- `ModBridgeCache` introduced to avoid repeated hot-path mod detection checks.
- Deprecated config toggles retained only as compatibility placeholders for removed features.

### Consolidation Series Continued (Section 12)

- Pass #2 complete: `DnsCacheUtil` unifies DNS enable/persist/host guards and connection hooks.
- Follow-up complete: DNS mixins moved under `mixin.network`; stale manager entries/comments removed.
- Passes #3-#7 complete: package re-org across `mixin.performance`, `mixin.client`, `mixin.integration`; HUD mixin merge and flattening complete.
- Pass #8 complete: ELB types moved from `com.iafenvoy.elb.*` to `com.alexxiconify.rustmc`.

### Additional Completed Reliability + UX Items

- DH fallback visibility and camera-minus culling behavior stabilized.
- Player-anchored frustum/cave updates prevent freecam/entity-camera drift.
- DH symbol availability and API-shape diagnostics now cached/logged once.
- DH JNI array handoffs use defensive snapshots.
- Native metric surface now live in HUD/Mod Menu with explicit status text.
- Keybind/category cleanup complete (`rustmc:keybinds`), timing overlay text-only.
- Dripstone native culling plumbing removed from config/UI.
- Particle spawn culling now uses cached squared cutoff distance at 20Hz.
- `:versions:mc1_21_11` local Xaero jar assumptions removed.

## Active Priorities (Same Canonical Plan as Roadmap)

### Now

1. Frustum + DH reliability checks on edge camera/FOV/world-join scenes.
2. JNI crossing hygiene (batch where measured; keep Java when faster).
3. Config/compat cleanup (remove dead placeholders/accessors/suppressions).
4. Native lighting/packet/chunk expansion only with profiling evidence.
5. Chunk ingest offload rollout (preview-only): add receive hook, then verify correctness + pacing.

### Next

1. Cache locality profiling (`MatrixMixin`, render-state lookups, JOML layout).
2. Debug visibility hooks (frustum counters, ratios, optional JNI timing).
3. Worldgen candidate analysis for Rust/WGPU batchable sampling paths.

### Future

1. Screen/HUD pass (`SplashOverlayMixin`, `LevelLoadingScreenMixin`, `WindowMixin`).
2. Chunk/mesh pipeline locality and allocation reduction.
3. JNI call-site lookup/caching micro-optimization.
4. Lock-free replacement only on proven contention.
5. Additional structure consolidation with behavior parity.
6. Backlog: selective `serde_json` bridge, benchmark scene expansion, optional extra Mod Menu stats.
7. Worldgen offload prototypes only with parity harness + safe recovery path.

## Validation Gates

- No frustum/LOD correctness regressions.
- Frame time lower or unchanged in target scene.
- Measured CPU/allocation reduction on touched hot path.
- Lower lock contention where modified.
- Lower JNI crossing count/per-call overhead where native path is used.
- No new crash/fallback/compat break.
- Benchmark or debug evidence for impact.
- No new warnings/errors in touched files.

## Non-Goals / Guardrails

- Do not keep JNI paths slower than Java equivalents.
- Do not trade correctness for speed.
- Do not add config for removed features.
- Do not remove locking without contention data.
- Do not widen mutable shared state without measured gain.
- Do not add per-frame release logging.


---

Last Updated: April 24, 2026