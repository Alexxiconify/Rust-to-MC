# Completed Changes

Historical-first view of the same canonical fact set used by [`ROADMAP.md`](../ROADMAP.md). Both files now stay aligned, with different ordering.

## Reference Paths

- Mod API/decompile reference jars: `C:/Users/Taylor Allred/AppData/Roaming/PandoraLauncher/instances/1.21.11-1.minecraft/mods`
- Rollback archive: [`docs/rollback.md`](rollback.md)

## Current Snapshot (April 2026)

- Target version: Minecraft `1.21.11` (`:versions:mc1_21_11`).
- Architecture: client-only optimization surface; server-only mixins removed.
- Mixins: reduced from 30+ to 20 active.
- Threading: platform daemon threads on startup/preload/ping/prefetch in place of virtual-thread usage.
- Native bridge: guarded JNI offload with explicit Java fallbacks.

## Chronological Milestones

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

### Next

1. Cache locality profiling (`MatrixMixin`, render-state lookups, JOML layout).
2. Debug visibility hooks (frustum counters, ratios, optional JNI timing).

### Future

1. Screen/HUD pass (`SplashOverlayMixin`, `LevelLoadingScreenMixin`, `WindowMixin`).
2. Chunk/mesh pipeline locality and allocation reduction.
3. JNI call-site lookup/caching micro-optimization.
4. Lock-free replacement only on proven contention.
5. Additional structure consolidation with behavior parity.
6. Backlog: selective `serde_json` bridge, benchmark scene expansion, optional extra Mod Menu stats.

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

## Rollback Snapshot

- Target: `v1.0.3-a3` (`12e8073`)
- Source: `main` at `9cd2790`
- Backup branch: `backup/pre-rollback-v1.0.3-a3-20260412-210316`
- Commits removed: 13 (`v1.0.3-a3..9cd2790`)

---

Last Updated: April 18, 2026