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
- Single-pass (Apr 24): DH frustum proxy now handles more intersect argument shapes safely; mixin ownership gating corrected; JNI batch frustum wrappers now avoid duplicate fallback paths/empty-array churn; dead compat stub block removed from `ModBridge`.

## Now

1. Validate frustum/DH edge cases. (in progress: proxy arg-shape/null hardening landed)
2. Cut JNI crossings and allocations where payoff is real. (in progress: batch frustum wrapper consolidation landed)
3. Remove dead config, compat, and logging noise. (in progress: `ModBridge` dead compat stub sweep landed)
4. Expand native packet/chunk work only with profiling proof.
5. Keep chunk ingest preview gated and sampled.
6. If Rust path overhead is higher than Java on a hot path, prefer Java multicore/multithread optimization first.

## Next

1. Render cache locality.
2. Debug observability for cull ratios and JNI timing.
3. Worldgen candidate analysis for Rust/WGPU batchable work.

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

Last Updated: April 24, 2026