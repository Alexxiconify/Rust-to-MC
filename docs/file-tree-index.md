# File Tree Index

Fast map. Start here before search.

## Root

- `ROADMAP.md` — active plan; short current state only.
- `docs/completed-changes.md` — full chronological history.
- `docs/file-tree-index.md` — quick lookup map.
- `README.md` — project summary and run/build entry points.
- `ModInfo.md` — mod-facing metadata / notes.
- `build.gradle` — root Gradle build and Rust staging.
- `settings.gradle` — included modules / version layout.
- `gradle.properties` — shared build flags and versions.

## Java: `src/main/java/com/alexxiconify/rustmc/`

- `RustMC.java` — mod init, config load/save, world hooks.
- `RustMCClient.java` — client init, keybinds, overlay toggles.
- `NativeBridge.java` — JNI hub, fallbacks, stats, cache load.
- `RustMCConfig.java` — config fields, getters, setters.
- `ModMenuIntegration.java` — config UI and status text.
- `ModBridge.java` — mod-detection bridge.
- `ModBridgeCache.java` — cached compat presence checks.
- `NativeCache.java` — runtime cache helper state.
- `PreLaunchHandler.java` — early startup / extraction hooks.
- `PreLaunchWindow.java` — early loading UI hooks.
- `MixinManager.java` — mixin timing / grouping helpers.
- `ElbConfig.java` — early loading bar config.
- `VersionCompat.java` — version-specific compatibility helpers.

### `compat/`

- `DistantHorizonsCompat.java` — DH culling / frustum integration.
- `ScalableLuxCompat.java` — lighting compat hooks.
- Other compat classes — mod-specific routing and fallbacks.

### `mixin/`

- `BootstrapMixin.java` — bootstrap timing / load hooks.
- `BoxMixin.java` — geometry / culling helpers.
- `CrashReportSectionMixin.java` — crash-report context.
- `FrustumMixin.java` — frustum update / visibility hooks.
- `ParticleManagerMixin.java` — particle cull / tick trim.
- `client/ClientFrameMetricsMixin.java` — frame timing capture.
- `client/DebugHudMixin.java` — sparkline and pie-chart overlays.
- `client/ResourceReloadMixin.java` — resource reload hooks.
- `network/ClientPlayNetworkHandlerMixin.java` — chunk ingest preview hook.
- `network/MultiplayerScreenMixin.java` — server list / UI hooks.
- `network/ServerAddressMixin.java` — DNS / address handling.
- `network/ServerPingerMixin.java` — ping path optimization.
- `performance/` — hot-path mixins for CPU trim.
- `integration/` — mod compat mixins.
- `screen/` — loading / screen rendering mixins.

### `util/`

- `BlameLog.java` — startup timing log.
- `DnsCacheUtil.java` — DNS cache persistence helper.
- `NativeStatsRenderer.java` — JNI metrics HUD.
- `PieChartRenderer.java` — text timing overlay.
- `OverlayDragManager.java` — draggable overlay state.
- `RamBarRenderer.java` — RAM bar rendering.
- `RenderState.java` — shared render-state cache.

## Java resources: `src/main/resources/`

- `fabric.mod.json` — Fabric entry metadata.
- `rust-mc.mixins.json` — mixin config.
- `rust_mc_core.dll` — bundled native library copy.
- `assets/` — icons, lang, and other resource assets.

## Rust: `rust_mc_core/src/`

- `lib.rs` — JNI exports, global context, cross-module wiring.
- `config.rs` — native config helpers.
- `frustum.rs` — frustum tests and helpers.
- `lighting.rs` — lighting work path.
- `math.rs` — scalar/math helpers.
- `net.rs` — DNS / network helpers.
- `occlusion.rs` — DH occlusion buffer and tests.
- `particles.rs` — particle physics / cutoff logic.
- `pipeline.rs` — routing for GPU / compute paths.
- `wgpu_ao.rs` — AO compute path and buffer pool.
- `wgpu_mesher.rs` — LOD meshing path and buffer pool.
- `ao_compute.wgsl` — AO shader.
- `lod_mesher.wgsl` — mesher shader.

## Version modules: `versions/`

- `mc1_21_11/` — active target version module.
- `mc26_1_1/` — alternate version module.

## Search order

1. `docs/file-tree-index.md`
2. `ROADMAP.md`
3. `docs/completed-changes.md`
4. source file under `src/main/java/` or `rust_mc_core/src/`