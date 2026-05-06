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
- Keep vanilla behavior and stable pacing.
- JNI safety with explicit fallbacks.

## Reference Paths

- Mod API: `C:/Users/Taylor Allred/AppData/Roaming/PandoraLauncher/instances/1.21.11-1.minecraft/mods`
- File index: [`docs/file-tree-index.md`](docs/file-tree-index.md)

## Active Tasks

- [ ] Implement Chunk Ingest offloading to Rust
- [ ] Native lighting integration (Experimental)
- [ ] Worldgen candidate analysis for Rust/WGPU
- [ ] Screen/HUD path optimization

## Now

1. Expand native packet/chunk work only with profiling proof. (in progress: chunk ingest sampling increased to 1/8)
2. Keep chunk ingest preview gated and sampled.
3. Render cache locality improvements.
4. If Rust path overhead is higher than Java on a hot path, prefer Java multicore/multithread optimization first.

## Next

1. Worldgen candidate analysis for Rust/WGPU batchable work.
2. Add Java-side frustum fingerprinting to avoid calling the native frustum update when scene unchanged (highest ROI: eliminates JNI crossing entirely on stable frames).
3. Add a lightweight JNI query API (e.g., `rustFrustumNeedsUpdate(hash: long) -> boolean`) to allow cheap pre-flight checks from Java when immediate Java-side fingerprinting is infeasible.
4. Create a small Java snippet and sample integration to compute the native-compatible frustum hash (include float-to-bits canonicalization and matrix column ordering) and include it in docs.

## Future

1. Screen/HUD path optimization.
2. Chunk/mesh pipeline locality and allocation trim.
3. JNI lookup/cache micro-optimizations.
4. Lock-free work only on proven contention.
5. Extra structure consolidation only when behavior stays unchanged.
6. Worldgen offload prototypes only with parity harness and safe recovery path.
7. CI enforcement: `cargo clippy -- -D warnings` + Gradle build on PRs to keep zero-warning target.
8. End-to-end micro-bench harness (Java + native) for measuring JNI crossing cost, frustum/occlusion, and AO/Lod work under repeatable scenes.
9. Lighting batching: accept batched lighting updates or digests; add optional on-change checks to skip duplicate work.

### Short-term tactical improvements (low-risk)

- Batch JNI lighting updates and DNS/packet ops where possible.
- Where Java already filters by camera, prefer Java-side culling to avoid JNI.
- Add unit tests for frustum/occlusion equivalence to prevent regressions when optimizing math.

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

Last Updated: May 6, 2026 (Frustum short-circuit, occlusion cache, shader/mesher micro-opts)
