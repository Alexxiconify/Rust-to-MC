# Rust to MC Roadmap

Active plan only. History: [`docs/completed-changes.md`](docs/completed-changes.md). Index: [`docs/file-tree-index.md`](docs/file-tree-index.md).

## Findings & Actions (Priority)

**Hotspots:**
- `NativeBridge::processChunkData` — JNI + array copy overhead
- `frustum.rs::update_from_matrix` — culling math + coordinate transforms
- `particles.rs::ParticleTickDispatcher` — allocations + parallel overhead
- JNI boundary: frequent short crossings

**Short-term:**
1. Instrument chunk ingest & frustum paths (3–6h, medium risk)
2. Validate fused DH cull perf (2–4h, low risk)
3. Replace IntStream.parallel→manual partition (1–2h, low risk)
4. CI: cargo clippy + gradlew enforcement (2h, low risk)

## Scope

- Move hot client paths Java→Rust
- Keep vanilla behavior, mod compat, stable pacing
- JNI safe: explicit fallbacks, no hard crashes

## Current Status (May 1-6)

**Target:** MC 1.21.11, client-only.

**Complete:**
- [x] HUD consolidation → `DiagnosticHudRenderer`
- [x] Config streamline → `DiagnosticMode`
- [x] Keybind opts (F7 HUD cycle, F8 Sparkline)
- [x] Abs coord consistency (VP matrix→world at boundary)
- [x] DH fused: frustum+cave+occlusion in single JNI crossing
- [x] Frustum short-circuit: fingerprints skip rebuilds on static
- [x] Shader/mesher micro-opts: const hoisting, index simplification
- [x] Frame telemetry: Java ring buffer (no native collection)
- [x] Reflection-free packet accessors
- [x] Particle distance culling + hardware presets
- [x] Rust deps: wgpu 24.0, jni 0.22, glam 0.32
- [x] Thread-local GPU buffer pooling
- [x] Occlusion: depth-buffer→lightweight frustum+AABB+cache
- [x] DH compat: visibility cache (LRU, 8K max), fused planes

**Gated:**
- Chunk ingest: preview, `enableChunkIngestOffload`, 1/8 sampled
- Native lighting: experimental, user-controlled coexist

## Q2 2026: Optimize & Maintain

- Chunk ingest: expand w/ profiling proof only (1/8 active)
- Keep sampled + gated
- Cache locality + alloc trim
- If Rust overhead > Java: prefer Java multithread first

## Next

1. Worldgen offload candidates (WGPU batchable)
2. Java frustum hash→skip native on static scene (highest ROI)
3. Lightweight JNI pre-flight: `rustFrustumNeedsUpdate(hash: long) → bool`
4. Java snippet + docs (float-to-bits, matrix column order)

## Future

- Screen/HUD path opt
- Chunk/mesh pipeline locality + alloc trim
- JNI lookup/cache micro-opts
- Lock-free on proven contention only
- Structure consolidation on equivalence only
- Worldgen offload w/ parity harness + fallback
- **CI enforcement:** `cargo clippy -- -D warnings` + Gradle (zero-warning)
- End-to-end micro-bench (Java+native, repeatable scenes)
- Lighting batch: accept digest, skip on-change

## Validation Gates

- No frustum/LOD regression
- Frame time ≥ target in scenes
- Lower CPU, alloc, or JNI cost
- No crash, fallback, or compat break
- Zero new warnings/errors

## Non-Goals

- JNI ≮ Java speed
- Correctness > speed always
- No removed feature config
- No unbounded shared state
- No per-frame release logging

---

Last: May 6 (Consolidation + env types + rustBatchCull refactor)